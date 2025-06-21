// TeknosaTicketSystem.java
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
    private JTextArea descriptionArea, outputArea;
    private JComboBox<String> priorityCombo, statusCombo;
    private JButton submitButton, clearButton, loginButton, registerButton, deleteButton, assignButton;
    private JPanel loginPanel, ticketPanel;
    private CardLayout cardLayout;
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
        setupDatabase();
    }

    private void initComponents() {
        titleField = new JTextField(30);
        descriptionArea = new JTextArea(8, 30);
        descriptionArea.setLineWrap(true);
        priorityCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
        statusCombo = new JComboBox<>(new String[]{"NUEVO", "ASIGNADO", "EN_PROCESO", "RESUELTO"});
        submitButton = new JButton("Crear Ticket");
        clearButton = new JButton("Limpiar");
        deleteButton = new JButton("Eliminar Ticket");
        assignButton = new JButton("Asignar Ticket");
        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Iniciar Sesión");
        registerButton = new JButton("Crear Cuenta");
        ((AbstractDocument)titleField.getDocument()).setDocumentFilter(new ValidationFilter());
        ((AbstractDocument)descriptionArea.getDocument()).setDocumentFilter(new ValidationFilter());
    }

    private void setupLayout() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);

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
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        loginPanel.add(loginButton, gbc);
        gbc.gridy = 3;
        loginPanel.add(registerButton, gbc);

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
            if (validateFields()) createTicket();
        });
        clearButton.addActionListener(e -> clearFields());
        registerButton.addActionListener(e -> showRegistrationDialog());
        deleteButton.addActionListener(e -> deleteTicket());
        assignButton.addActionListener(e -> assignTicket());
    }

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

    private void authenticateUser() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String sql = "SELECT id_usuario, nombre, es_administrador FROM usuarios WHERE email = ? AND password = SHA2(?, 256)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentUserId = rs.getInt("id_usuario");
                isAdmin = rs.getBoolean("es_administrador");
                statusCombo.setEnabled(isAdmin);
                cardLayout.show(getContentPane(), "tickets");
                loadTickets();
                JOptionPane.showMessageDialog(this, "Bienvenido " + rs.getString("nombre") + (isAdmin ? " (Admin)" : ""));
            } else {
                JOptionPane.showMessageDialog(this, "Credenciales incorrectas");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

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

    private void createTicket() {
        String sql = "INSERT INTO tickets (titulo, descripcion, prioridad, estado, id_cliente) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titleField.getText());
            pstmt.setString(2, descriptionArea.getText());
            pstmt.setString(3, (String) priorityCombo.getSelectedItem());
            pstmt.setString(4, (String) statusCombo.getSelectedItem());
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

    private void deleteTicket() {
        String input = JOptionPane.showInputDialog(this, "ID del ticket a eliminar:");
        if (input != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tickets WHERE id_ticket = ?")) {
                pstmt.setInt(1, Integer.parseInt(input));
                pstmt.executeUpdate();
                logAction("TICKET", "Ticket eliminado", currentUserId);
                loadTickets();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void assignTicket() {
        JTextField idField = new JTextField();
        JTextField userField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("ID del Ticket:"));
        panel.add(idField);
        panel.add(new JLabel("ID de usuario asignado:"));
        panel.add(userField);
        int result = JOptionPane.showConfirmDialog(this, panel, "Asignar Ticket", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE tickets SET id_soporte = ?, estado = 'ASIGNADO' WHERE id_ticket = ?")) {
                pstmt.setInt(1, Integer.parseInt(userField.getText()));
                pstmt.setInt(2, Integer.parseInt(idField.getText()));
                pstmt.executeUpdate();
                logAction("TICKET", "Ticket asignado", currentUserId);
                loadTickets();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void loadTickets() {
        outputArea.setText("");
        String sql = isAdmin ?
            "SELECT t.*, u.nombre as cliente FROM tickets t JOIN usuarios u ON t.id_cliente = u.id_usuario ORDER BY t.fecha_creacion DESC" :
            "SELECT t.*, u.nombre as cliente FROM tickets t JOIN usuarios u ON t.id_cliente = u.id_usuario WHERE t.id_cliente = ? ORDER BY t.fecha_creacion DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!isAdmin) pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                outputArea.append(String.format(
                    "Ticket #%d: %s\nCliente: %s | Prioridad: %s | Estado: %s\nCreado: %s\nDescripción: %s\n-----------------------\n",
                    rs.getInt("id_ticket"), rs.getString("titulo"), rs.getString("cliente"),
                    rs.getString("prioridad"), rs.getString("estado"),
                    rs.getTimestamp("fecha_creacion").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    rs.getString("descripcion")
                ));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

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

    private void showRegistrationDialog() {
        JTextField nameField = new JTextField();
        JTextField regEmail = new JTextField();
        JPasswordField regPass = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre:")); panel.add(nameField);
        panel.add(new JLabel("Email:")); panel.add(regEmail);
        panel.add(new JLabel("Contraseña:")); panel.add(regPass);
        int result = JOptionPane.showConfirmDialog(this, panel, "Registro de Usuario", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO usuarios(nombre, email, password, es_administrador) VALUES (?, ?, SHA2(?, 256), false)")) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, regEmail.getText());
                pstmt.setString(3, new String(regPass.getPassword()));
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error de registro: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        titleField.setText("");
        descriptionArea.setText("");
        priorityCombo.setSelectedIndex(1);
        statusCombo.setSelectedIndex(0);
    }

    private class ValidationFilter extends DocumentFilter {
        private final Pattern pattern = Pattern.compile("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ.,;:()@-]");
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && !pattern.matcher(string).find()) {
                super.insertString(fb, offset, string, attr);
            } else {
                JOptionPane.showMessageDialog(null, "Caracteres no permitidos.");
            }
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || !pattern.matcher(text).find()) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                JOptionPane.showMessageDialog(null, "Caracteres no permitidos.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TeknosaTicketSystem().setVisible(true));
    }
} 
