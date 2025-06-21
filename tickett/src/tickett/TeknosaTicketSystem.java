package tickett; // ¡Importante! El paquete ahora es 'tickett'

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime; // Asegúrate de que tu JDK sea Java 8 o superior
import java.time.format.DateTimeFormatter; // Asegúrate de que tu JDK sea Java 8 o superior
import java.util.regex.Pattern;

// YA NO NECESITAS ESTA LÍNEA DE IMPORTACIÓN porque DatabaseConnection
// ahora está en el mismo paquete 'tickett'.
// import database.DatabaseConnection; 

public class TeknosaTicketSystem extends JFrame {

    // Componentes de la interfaz
    private JTextField titleField, emailField;
    private JPasswordField passwordField;
    private JTextArea descriptionArea, outputArea;
    private JComboBox<String> priorityCombo, statusCombo;
    private JButton submitButton, clearButton, loginButton;
    private JPanel loginPanel, ticketPanel;
    private CardLayout cardLayout;

    // Variables de estado
    private boolean isAdmin = false;
    private int currentUserId;

    public TeknosaTicketSystem() {
        setTitle("TEKNOSA - Sistema de Tickets");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        setupLayout();
        setupValidations();
        setupDatabase(); // Llama a la configuración de la base de datos al inicio
    }

    private void initComponents() {
        // Configuración de los componentes
        titleField = new JTextField(30);
        descriptionArea = new JTextArea(8, 30);
        descriptionArea.setLineWrap(true);
        priorityCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
        statusCombo = new JComboBox<>(new String[]{"NUEVO", "ASIGNADO", "EN_PROCESO", "RESUELTO"});
        submitButton = new JButton("Crear Ticket");
        clearButton = new JButton("Limpiar");
        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);

        // Componentes de login
        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Iniciar Sesión");

        // Configurar filtros de validación
        ((AbstractDocument)titleField.getDocument()).setDocumentFilter(new ValidationFilter());
        ((AbstractDocument)descriptionArea.getDocument()).setDocumentFilter(new ValidationFilter());
    }

    private void setupLayout() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        // Panel de Login
        loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        loginPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Contraseña:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        loginPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        loginPanel.add(loginButton, gbc);

        // Panel de Tickets
        ticketPanel = new JPanel(new BorderLayout(10, 10));
        ticketPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Título:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Descripción:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Prioridad:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        formPanel.add(priorityCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Estado:"), gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        formPanel.add(statusCombo, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        buttonPanel.add(submitButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        ticketPanel.add(formPanel, BorderLayout.NORTH);
        ticketPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(loginPanel, "login");
        add(ticketPanel, "tickets");
    }

    private void setupValidations() {
        loginButton.addActionListener(e -> authenticateUser());

        submitButton.addActionListener(e -> {
            if (validateFields()) {
                createTicket();
            }
        });

        clearButton.addActionListener(e -> clearFields());
    }

    private void setupDatabase() {
        try {
            // Llama al método getConnection de tu clase DatabaseConnection
            // No se necesita importar porque está en el mismo paquete 'tickett'
            DatabaseConnection.getConnection(); 
            logAction("SISTEMA", "Inicio de aplicación", currentUserId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error al conectar con la base de datos: " + e.getMessage() + "\nAsegúrate de que MySQL esté corriendo y las credenciales sean correctas.", 
                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            // También es buena idea imprimir el stack trace para depuración
            e.printStackTrace();
        }
         // Añade un WindowListener para cerrar la conexión cuando la ventana se cierre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseConnection.closeConnection();
            }
        });
    }

    private void authenticateUser() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Email y contraseña son requeridos", 
                "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Consulta SQL para autenticar al usuario usando SHA256 para la contraseña
        String sql = "SELECT id_usuario, nombre, es_administrador FROM usuarios WHERE email = ? AND password = SHA2(?, 256)";

        try (Connection conn = DatabaseConnection.getConnection(); // Obtiene la conexión
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("id_usuario");
                isAdmin = rs.getBoolean("es_administrador");
                String nombre = rs.getString("nombre");

                // Configurar interfaz según el rol
                statusCombo.setEnabled(isAdmin); // Solo los administradores pueden cambiar el estado

                logAction("USUARIO", "Inicio de sesión exitoso", currentUserId);

                cardLayout.show(getContentPane(), "tickets"); // Cambia al panel de tickets
                loadTickets(); // Carga los tickets para el usuario/administrador

                JOptionPane.showMessageDialog(this, 
                    "Bienvenido " + nombre + (isAdmin ? " (Administrador)" : ""), 
                    "Inicio de Sesión Exitoso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Credenciales incorrectas. Verifique su email y contraseña.", 
                    "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
                logAction("USUARIO", "Intento de inicio de sesión fallido", 0); // User ID 0 para fallidos
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error de base de datos durante la autenticación: " + e.getMessage(), 
                "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private boolean validateFields() {
        if (titleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El título no puede estar vacío", 
                "Campo Requerido", JOptionPane.ERROR_MESSAGE);
            titleField.requestFocus();
            return false;
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "La descripción no puede estar vacía", 
                "Campo Requerido", JOptionPane.ERROR_MESSAGE);
            descriptionArea.requestFocus();
            return false;
        }

        return true;
    }

    private void createTicket() {
        String title = titleField.getText();
        String description = descriptionArea.getText();
        String priority = (String) priorityCombo.getSelectedItem();
        String status = (String) statusCombo.getSelectedItem(); // El estado inicial para un nuevo ticket

        // SQL para insertar un nuevo ticket
        String sql = "INSERT INTO tickets (titulo, descripcion, prioridad, estado, id_cliente) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); // Obtiene la conexión
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, priority);
            pstmt.setString(4, status);
            pstmt.setInt(5, currentUserId); // Asigna el ticket al usuario actual

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Si el ticket se creó, registra la acción, limpia los campos y recarga la lista de tickets
                logAction("TICKET", "Ticket creado", currentUserId);
                clearFields();
                loadTickets();
                JOptionPane.showMessageDialog(this, 
                    "Ticket creado exitosamente", 
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error al crear ticket: " + e.getMessage(), 
                "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadTickets() {
        outputArea.setText(""); // Limpia el área de texto antes de cargar nuevos tickets

        // SQL para cargar tickets. Diferente para administradores y usuarios normales.
        String sql = isAdmin ? 
            "SELECT t.*, u.nombre as cliente FROM tickets t JOIN usuarios u ON t.id_cliente = u.id_usuario ORDER BY t.fecha_creacion DESC" :
            "SELECT t.*, u.nombre as cliente FROM tickets t JOIN usuarios u ON t.id_cliente = u.id_usuario WHERE t.id_cliente = ? ORDER BY t.fecha_creacion DESC";

        try (Connection conn = DatabaseConnection.getConnection(); // Obtiene la conexión
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (!isAdmin) {
                // Si no es admin, filtra por el ID del usuario actual
                pstmt.setInt(1, currentUserId);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Formatea la información de cada ticket para mostrarla en el outputArea
                String ticketInfo = String.format(
                    "Ticket #%d: %s\n" +
                    "Cliente: %s | Prioridad: %s | Estado: %s\n" +
                    "Creado: %s\n" +
                    "Descripción: %s\n" +
                    "---------------------------------\n",
                    rs.getInt("id_ticket"),
                    rs.getString("titulo"),
                    rs.getString("cliente"),
                    rs.getString("prioridad"),
                    rs.getString("estado"),
                    rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    rs.getString("descripcion")
                );

                outputArea.append(ticketInfo); // Añade la información al área de salida
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error al cargar tickets: " + e.getMessage(), 
                "Error SQL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Método para registrar acciones en la tabla de auditoría
    private void logAction(String entity, String action, int userId) {
        String sql = "INSERT INTO auditoria (accion, entidad, id_usuario, detalles, ip_origen) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); // Obtiene la conexión
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, action);
            pstmt.setString(2, entity);
            // Si userId es 0 (ej. para inicio de sesión fallido), inserta NULL en id_usuario si la columna lo permite
            if (userId == 0) {
                pstmt.setNull(3, Types.INTEGER);
            } else {
                pstmt.setInt(3, userId);
            }
            pstmt.setString(4, "Acción desde la interfaz gráfica");
            pstmt.setString(5, "127.0.0.1"); // IP de ejemplo (podrías obtener la IP real si fuera una app de red)

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar auditoría: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearFields() {
        titleField.setText("");
        descriptionArea.setText("");
        priorityCombo.setSelectedIndex(1); // Prioridad Media por defecto
        statusCombo.setSelectedIndex(0); // Estado Nuevo
    }

    // Filtro para validar caracteres especiales en los campos de texto
    private class ValidationFilter extends DocumentFilter {
        // Permite letras (mayúsculas/minúsculas, acentuadas, ñ), números, espacios,
        // y algunos signos de puntuación comunes: .,;:()@-
        private final Pattern pattern = Pattern.compile("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ.,;:()@-]");

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) 
            throws BadLocationException {
            if (string == null || string.isEmpty()) {
                return; // Nothing to insert
            }
            // Solo inserta si el texto no contiene caracteres no permitidos
            if (!pattern.matcher(string).find()) {
                super.insertString(fb, offset, string, attr);
            } else {
                // Opcional: Notificar al usuario que se han filtrado caracteres
                // System.out.println("Caracteres no permitidos detectados y filtrados: " + string);
                JOptionPane.showMessageDialog(null, 
                    "Se han omitido caracteres no permitidos. Solo se permiten letras, números, espacios y los siguientes símbolos: .,;:()@-", 
                    "Advertencia de Entrada", JOptionPane.WARNING_MESSAGE);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
            throws BadLocationException {
            if (text == null || text.isEmpty()) {
                super.replace(fb, offset, length, text, attrs); // Allow deletion or empty replacement
                return;
            }
            // Solo reemplaza si el texto no contiene caracteres no permitidos
            if (!pattern.matcher(text).find()) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                // Opcional: Notificar al usuario
                // System.out.println("Caracteres no permitidos detectados y filtrados durante el reemplazo: " + text);
                 JOptionPane.showMessageDialog(null, 
                    "Se han omitido caracteres no permitidos. Solo se permiten letras, números, espacios y los siguientes símbolos: .,;:()@-", 
                    "Advertencia de Entrada", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        // Ejecuta la aplicación Swing en el Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            TeknosaTicketSystem system = new TeknosaTicketSystem();
            system.setVisible(true);
        });
    }
}