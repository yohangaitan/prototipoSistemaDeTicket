package tickett;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class TeknosaTicketSystem extends JFrame {

    private JTextField titleField, emailField;
    private JPasswordField passwordField;
    private JTextArea descriptionArea;
    private JTextPane outputArea; // Cambiado de JTextArea a JTextPane
    private JComboBox<String> priorityCombo, statusCombo;
    private JButton submitButton, clearButton, loginButton, registerButton,
                    deleteButton, assignButton, editButton, chatButton,
                    logoutButton, viewHistoryButton;
    private JPanel loginPanel, ticketPanel;
    private CardLayout cardLayout;
    private boolean isAdmin = false;
    private int currentUserId;
    private JCheckBox showPasswordCheckBox; // AÑADIDO: CheckBox para mostrar contraseña

    // ======================== MÉTODOS DE NAVEGACIÓN =====================
    private void logout() {
        clearFields();
        emailField.setText("");
        passwordField.setText("");
        currentUserId = 0;
        isAdmin = false;
        cardLayout.show(getContentPane(), "login");
        outputArea.setText("");
        updateUIBasedOnRole();
    }

    // ======================== CRONOLOGÍA ===============================
    private void mostrarCronologia() {
        String input = JOptionPane.showInputDialog(this, "ID del ticket:");
        if (input != null) {
            try {
                int ticketId = Integer.parseInt(input);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM cronologia WHERE id_ticket = ? ORDER BY fecha")) {
                    pstmt.setInt(1, ticketId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        StringBuilder history = new StringBuilder("Cronología del Ticket #" + input + ":\n\n");
                        while (rs.next()) {
                            history.append(String.format("[%s] %s ➝ %s (Usuario ID: %d)\n",
                                rs.getTimestamp("fecha").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                rs.getString("estado_anterior"), rs.getString("estado_nuevo"), rs.getInt("id_usuario")));
                        }
                        JOptionPane.showMessageDialog(this, history.toString());
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID de ticket inválido. Por favor, introduzca un número.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar cronología: " + ex.getMessage());
            }
        }
    }

    // ======================== ACTUALIZAR ESTADO DE TICKET ========================
    private void updateTicketStatus() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden cambiar el estado de un ticket.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "ID del ticket para cambiar estado:");
        if (input != null) {
            try {
                int ticketId = Integer.parseInt(input);

                // Obtener estado actual
                String estadoAnterior = "";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement sel = conn.prepareStatement("SELECT estado FROM tickets WHERE id_ticket = ?")) {
                    sel.setInt(1, ticketId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            estadoAnterior = rs.getString("estado");
                        } else {
                            JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado.");
                            return;
                        }
                    }
                }

                // Actualizar nuevo estado
                String nuevoEstado = (String) statusCombo.getSelectedItem();
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement update = conn.prepareStatement("UPDATE tickets SET estado = ? WHERE id_ticket = ?")) {
                    update.setString(1, nuevoEstado);
                    update.setInt(2, ticketId);
                    update.executeUpdate();
                }

                // Guardar en cronología
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement crono = conn.prepareStatement(
                         "INSERT INTO cronologia(id_ticket, estado_anterior, estado_nuevo, id_usuario) VALUES (?, ?, ?, ?)")) {
                    crono.setInt(1, ticketId);
                    crono.setString(2, estadoAnterior);
                    crono.setString(3, nuevoEstado);
                    crono.setInt(4, currentUserId);
                    crono.executeUpdate();
                }

                logAction("TICKET", "Cambio de estado", currentUserId);
                loadTickets();
                JOptionPane.showMessageDialog(this, "Estado actualizado y registrado en cronología.");

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID de ticket inválido. Por favor, introduzca un número.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al actualizar estado: " + e.getMessage());
            }
        }
    }

    // ======================== CONSTRUCTOR ================================
    public TeknosaTicketSystem() {
        setTitle("TEKNOSA - Sistema de Tickets");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        setupLayout();
        setupValidations();
        setupDatabase();
        updateUIBasedOnRole();
    }

    // ======================== INICIALIZAR COMPONENTES ====================
    private void initComponents() {
        titleField = new JTextField(30);
        descriptionArea = new JTextArea(8, 30);
        descriptionArea.setLineWrap(true);
        priorityCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
        statusCombo = new JComboBox<>(new String[]{"NUEVO", "ASIGNADO", "EN_PROCESO", "RESUELTO", "COMPLETO"});
        submitButton = new JButton("Crear Ticket");
        clearButton = new JButton("Limpiar");
        deleteButton = new JButton("Eliminar Ticket");
        assignButton = new JButton("Asignar Ticket");
        editButton = new JButton("Editar Ticket");
        chatButton = new JButton("Iniciar Chat");

        outputArea = new JTextPane();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);
        showPasswordCheckBox = new JCheckBox("Mostrar contraseña"); // AÑADIDO: Inicialización del CheckBox
        loginButton = new JButton("Iniciar Sesión");
        logoutButton = new JButton("Cerrar Sesión");
        registerButton = new JButton("Crear Cuenta");
        viewHistoryButton = new JButton("Ver Cronología");

        ((AbstractDocument)titleField.getDocument()).setDocumentFilter(new ValidationFilter());
        ((AbstractDocument)descriptionArea.getDocument()).setDocumentFilter(new ValidationFilter());
    }

    // ======================== CONFIGURAR LAYOUT ==========================
    private void setupLayout() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        // --- Panel de Login ---
        loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(emailField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        loginPanel.add(passwordField, gbc);
        gbc.gridx = 1; gbc.gridy = 2; // AÑADIDO: Posición del CheckBox
        gbc.anchor = GridBagConstraints.WEST; // Alinear a la izquierda
        loginPanel.add(showPasswordCheckBox, gbc); // AÑADIDO: Añadir el CheckBox al panel

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(loginButton, gbc);
        gbc.gridy = 4;
        loginPanel.add(registerButton, gbc);

        // --- Panel de Tickets ---
        ticketPanel = new JPanel(new BorderLayout(10, 10));
        ticketPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel formPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Título:"), gbc);
        gbc.gridx = 1;
        formPanel.add(titleField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(descriptionArea), gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Prioridad:"), gbc);
        gbc.gridx = 1;
        formPanel.add(priorityCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusCombo, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        buttonPanel.add(submitButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(assignButton);
        buttonPanel.add(editButton);
        buttonPanel.add(chatButton);
        buttonPanel.add(viewHistoryButton);
        buttonPanel.add(logoutButton);


        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);
        ticketPanel.add(formPanel, BorderLayout.NORTH);
        ticketPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(loginPanel, "login");
        add(ticketPanel, "tickets");
    }

    // ======================== CONFIGURAR VALIDACIONES Y LISTENERS ========================
    private void setupValidations() {
        viewHistoryButton.addActionListener(e -> mostrarCronologia());
        statusCombo.addActionListener(e -> updateTicketStatus());
        loginButton.addActionListener(e -> authenticateUser());
        logoutButton.addActionListener(e -> logout());
        submitButton.addActionListener(e -> {
            if (validateFields()) createTicket();
        });
        clearButton.addActionListener(e -> clearFields());
        registerButton.addActionListener(e -> showRegistrationDialog());
        deleteButton.addActionListener(e -> deleteTicket());
        assignButton.addActionListener(e -> assignTicket());
        // AÑADIDO: Listener para el CheckBox de mostrar contraseña
        showPasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPasswordCheckBox.isSelected()) {
                    passwordField.setEchoChar((char) 0); // Muestra caracteres
                } else {
                    passwordField.setEchoChar('*'); // Oculta caracteres
                }
            }
        });
    }

    // ======================== CONFIGURAR BASE DE DATOS ========================
    private void setupDatabase() {
        try {
            DatabaseConnection.getConnection();
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    DatabaseConnection.closeConnection();
                }
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al conectar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ======================== AUTENTICACIÓN DE USUARIO ========================
    private void authenticateUser() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        // MODIFICADO: Seleccionar apellido también
        String sql = "SELECT id_usuario, nombre, apellido, es_administrador FROM usuarios WHERE email = ? AND password = SHA2(?, 256)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentUserId = rs.getInt("id_usuario");
                    isAdmin = rs.getBoolean("es_administrador");
                    
                    updateUIBasedOnRole();
                    
                    cardLayout.show(getContentPane(), "tickets");
                    String userName = rs.getString("nombre"); 
                    String userLastName = rs.getString("apellido"); // AÑADIDO: Obtener el apellido
                    loadTickets();
                    // MODIFICADO: Mostrar también el apellido
                    JOptionPane.showMessageDialog(this, "Bienvenido " + userName + " " + userLastName + (isAdmin ? " (Admin)" : ""));
                } else {
                    JOptionPane.showMessageDialog(this, "Credenciales incorrectas");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

    // ======================== ACTUALIZAR LA INTERFAZ DE USUARIO SEGÚN EL ROL ========================
    private void updateUIBasedOnRole() {
        // Campos de creación de ticket (comunes a ambos roles, pero prioridad editable solo por admin)
        titleField.setEditable(true);
        descriptionArea.setEditable(true);
        submitButton.setVisible(true);
        clearButton.setVisible(true);

        // Ocultar/deshabilitar para usuarios normales
        deleteButton.setVisible(isAdmin);
        assignButton.setVisible(isAdmin);
        editButton.setVisible(isAdmin); // Por defecto visible para admin
        chatButton.setVisible(true); // Asumimos chat es visible para ambos, pero la lógica interna puede variar

        statusCombo.setEnabled(isAdmin);
        priorityCombo.setEnabled(isAdmin);
        viewHistoryButton.setVisible(isAdmin);
    }

    // ======================== VALIDACIÓN DE CAMPOS ========================
    private boolean validateFields() {
        if (titleField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El título no puede estar vacío");
            return false;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción no puede estar vacía");
            return false;
        }
        return true;
    }

    // ======================== CREAR TICKET ========================
    private void createTicket() {
        String sql = "INSERT INTO tickets (titulo, descripcion, prioridad, estado, id_cliente) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titleField.getText());
            pstmt.setString(2, descriptionArea.getText());
            pstmt.setString(3, (String) priorityCombo.getSelectedItem());
            pstmt.setString(4, "NUEVO");
            pstmt.setInt(5, currentUserId);
            pstmt.executeUpdate();
            logAction("TICKET", "Ticket creado", currentUserId);
            clearFields();
            loadTickets();
            JOptionPane.showMessageDialog(this, "Ticket creado exitosamente");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

    // ======================== ELIMINAR TICKET ========================
    private void deleteTicket() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden eliminar tickets.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "ID del ticket a eliminar:");
        if (input != null) {
            try {
                int ticketId = Integer.parseInt(input);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tickets WHERE id_ticket = ?")) {
                    pstmt.setInt(1, ticketId);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        logAction("TICKET", "Ticket eliminado", currentUserId);
                        loadTickets();
                        JOptionPane.showMessageDialog(this, "Ticket #" + ticketId + " eliminado exitosamente.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado.");
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID de ticket inválido. Por favor, introduzca un número.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar ticket: " + e.getMessage());
            }
        }
    }

    // ======================== ASIGNAR TICKET ========================
    private void assignTicket() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden asignar tickets.");
            return;
        }
        JTextField idField = new JTextField();
        JTextField userField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("ID del Ticket:"));
        panel.add(idField);
        panel.add(new JLabel("ID de usuario asignado:"));
        panel.add(userField);
        int result = JOptionPane.showConfirmDialog(this, panel, "Asignar Ticket", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int ticketId = Integer.parseInt(idField.getText());
                int userIdToAssign = Integer.parseInt(userField.getText());

                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE tickets SET id_soporte = ?, estado = 'ASIGNADO' WHERE id_ticket = ?")) {
                    pstmt.setInt(1, userIdToAssign);
                    pstmt.setInt(2, ticketId);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        logAction("TICKET", "Ticket asignado", currentUserId);
                        loadTickets();
                        JOptionPane.showMessageDialog(this, "Ticket #" + ticketId + " asignado a usuario ID " + userIdToAssign + " y estado actualizado a ASIGNADO.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado o usuario de soporte inválido.");
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID de ticket o usuario inválido. Por favor, introduzca números.");
            } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al asignar ticket: " + e.getMessage());
            }
        }
    }

    // ======================== CARGAR TICKETS Y MOSTRAR INDICADORES ========================
    private void loadTickets() {
        outputArea.setText("");
        // MODIFICADO: Seleccionar apellido del cliente también
        String sql = isAdmin ?
            "SELECT t.*, u.nombre as cliente, u.apellido as cliente_apellido FROM tickets t JOIN usuarios u ON t.id_cliente = u.id_usuario ORDER BY t.fecha_creacion DESC" :
            "SELECT t.*, u.nombre as cliente, u.apellido as cliente_apellido FROM tickets t JOIN usuarios u ON t.id_cliente = u.id_usuario WHERE t.id_cliente = ? ORDER BY t.fecha_creacion DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!isAdmin) pstmt.setInt(1, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("estado");
                    String indicator = "";
                    switch (status.toUpperCase()) {
                        case "NUEVO":
                            indicator = "🔴 "; // Rojo para no completa/nueva
                            break;
                        case "ASIGNADO":
                        case "EN_PROCESO":
                            indicator = "🟡 "; // Amarillo para en proceso
                            break;
                        case "RESUELTO":
                        case "COMPLETO":
                            indicator = "🟢 "; // Verde para atendida/resuelta/completa
                            break;
                        default:
                            indicator = ""; // Sin indicador por defecto
                    }

                    outputArea.setText(outputArea.getText() + String.format(
                        "Ticket #%d: %s\nCliente: %s %s | Prioridad: %s | Estado: %s%s\nCreado: %s\nDescripción: %s\n-----------------------\n",
                        rs.getInt("id_ticket"), rs.getString("titulo"), 
                        rs.getString("cliente"), rs.getString("cliente_apellido"), // AÑADIDO: Mostrar apellido del cliente
                        rs.getString("prioridad"), indicator, status,
                        rs.getTimestamp("fecha_creacion").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        rs.getString("descripcion")
                    ));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

    // ======================== AUDITORÍA ========================
    private void logAction(String entity, String action, int userId) {
        String sql = "INSERT INTO auditoria (accion, entidad, id_usuario, detalles, ip_origen) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, action);
            pstmt.setString(2, entity);
            if (userId == 0) pstmt.setNull(3, Types.INTEGER); else pstmt.setInt(3, userId);
            pstmt.setString(4, "Acción desde la interfaz gráfica");
            pstmt.setString(5, "127.0.0.1");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error auditoría: " + e.getMessage());
        }
    }

    // ======================== REGISTRO DE USUARIO ========================
    private void showRegistrationDialog() {
        JTextField nameField = new JTextField();
        JTextField lastNameField = new JTextField(); // AÑADIDO: Nuevo campo para apellido
        JTextField regEmail = new JTextField();
        JPasswordField regPass = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre:")); panel.add(nameField);
        panel.add(new JLabel("Apellido:")); panel.add(lastNameField); // AÑADIDO: Añadir campo de apellido al diálogo
        panel.add(new JLabel("Email:")); panel.add(regEmail);
        panel.add(new JLabel("Contraseña:")); panel.add(regPass);
        int result = JOptionPane.showConfirmDialog(this, panel, "Registro de Usuario", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (regEmail.getText().trim().isEmpty() || !Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", regEmail.getText())) {
                JOptionPane.showMessageDialog(this, "Por favor, introduce un email válido.");
                return;
            }
            if (new String(regPass.getPassword()).trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "La contraseña no puede estar vacía.");
                return;
            }
            // AÑADIDO: Validación para nombre y apellido no vacíos (opcional, pero buena práctica)
            if (nameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre y el apellido no pueden estar vacíos.");
                return;
            }


            // MODIFICADO: Insertar apellido también en la sentencia SQL
            String sql = "INSERT INTO usuarios(nombre, apellido, email, password, es_administrador) VALUES (?, ?, ?, SHA2(?, 256), false)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, lastNameField.getText()); // AÑADIDO: Asignar el apellido
                pstmt.setString(3, regEmail.getText());
                pstmt.setString(4, new String(regPass.getPassword()));
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente. Ahora puedes iniciar sesión.");
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, "Error de registro: El email ya está registrado. " + e.getMessage());
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error de registro: " + e.getMessage());
            }
        }
    }

    // ======================== LIMPIAR CAMPOS ========================
    private void clearFields() {
        titleField.setText("");
        descriptionArea.setText("");
        priorityCombo.setSelectedIndex(1);
        statusCombo.setSelectedIndex(0);
    }

    // ======================== VALIDACIÓN DE TEXTO ========================
    private class ValidationFilter extends DocumentFilter {
        private final Pattern pattern = Pattern.compile("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ.,;:()@\\- ]");
        @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && !pattern.matcher(string).find()) {
                super.insertString(fb, offset, string, attr);
            } else {
                JOptionPane.showMessageDialog(null, "Caracteres no permitidos.");
            }
        }
        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || !pattern.matcher(text).find()) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                JOptionPane.showMessageDialog(null, "Caracteres no permitidos.");
            }
        }
    }

    // ======================== MAIN =======================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TeknosaTicketSystem().setVisible(true));
    }
}