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

    // ======================== MÉTODOS DE NAVEGACIÓN =====================
    private void logout() {
    // Remover temporalmente el listener
    statusCombo.removeActionListener(statusCombo.getActionListeners()[0]);
    
    // Limpiar campos y cambiar de panel
    clearFields();
    emailField.setText("");
    passwordField.setText("");
    currentUserId = 0;
    isAdmin = false;
    cardLayout.show(getContentPane(), "login");
    outputArea.setText("");
    
    // Volver a agregar el listener
    statusCombo.addActionListener(e -> updateTicketStatus());
}

    // ======================== CRONOLOGÍA ================================
    private void mostrarCronologia() {
        String input = JOptionPane.showInputDialog(this, "ID del ticket:");
        if (input != null) {
            try {
                int ticketId = Integer.parseInt(input);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT * FROM cronologia WHERE id_ticket = ? ORDER BY fecha")) {
                    pstmt.setInt(1, ticketId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        StringBuilder history = new StringBuilder("Cronología del Ticket #" + input + ":\n\n");
                        while (rs.next()) {
                            history.append(String.format("[%s] %s ➝ %s (Usuario ID: %d)\n",
                                rs.getTimestamp("fecha").toLocalDateTime().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                rs.getString("estado_anterior"), rs.getString("estado_nuevo"),
                                rs.getInt("id_usuario")));
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

    // ======================== ACTUALIZAR ESTADO =========================
    private void updateTicketStatus() {
        if (!isAdmin) return;
        
        String input = JOptionPane.showInputDialog(this, "ID del ticket para cambiar estado:");
        if (input != null) {
            try {
                int ticketId = Integer.parseInt(input);
                String estadoAnterior = "";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement sel = conn.prepareStatement(
                         "SELECT estado FROM tickets WHERE id_ticket = ?")) {
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
                String nuevoEstado = (String) statusCombo.getSelectedItem();
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement update = conn.prepareStatement(
                         "UPDATE tickets SET estado = ? WHERE id_ticket = ?")) {
                    update.setString(1, nuevoEstado);
                    update.setInt(2, ticketId);
                    update.executeUpdate();
                }
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
    
    private JCheckBox showPasswordCheckBox;


    // ======================== INICIALIZACIÓN UI =========================
    private void initComponents() {
        titleField = new JTextField(30);
        descriptionArea = new JTextArea(8, 30);
        descriptionArea.setLineWrap(true);
        priorityCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
        statusCombo = new JComboBox<>(new String[]{"NUEVO", "EN PROCESO", "COMPLETO"});

        submitButton = new JButton("Crear Ticket");
        clearButton = new JButton("Limpiar");
        deleteButton = new JButton("Eliminar Ticket");
        assignButton = new JButton("Asignar Ticket");
        editButton = new JButton("Modificar Ticket");
        chatButton = new JButton("Chat");
        logoutButton = new JButton("Cerrar Sesión");
        viewHistoryButton = new JButton("Ver Cronología");

        outputArea = new JTextPane(); // Cambiado a JTextPane
        outputArea.setEditable(false);
        outputArea.setContentType("text/html");

        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);
        showPasswordCheckBox = new JCheckBox("Mostrar contraseña");
        loginButton = new JButton("Iniciar Sesión");
        registerButton = new JButton("Crear Cuenta");

        ((AbstractDocument) titleField.getDocument()).setDocumentFilter(new ValidationFilter());
        ((AbstractDocument) descriptionArea.getDocument()).setDocumentFilter(new ValidationFilter());
    }

    // ======================== LAYOUT =====================================
    private void setupLayout() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        // Panel de Login
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
        loginPanel.add(showPasswordCheckBox, gbc);
        gbc.gridy = 3;
        loginPanel.add(loginButton, gbc);
        gbc.gridy = 4;
        loginPanel.add(registerButton, gbc);

        // Panel de Tickets
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
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(assignButton);
        buttonPanel.add(chatButton);
        buttonPanel.add(logoutButton);
        buttonPanel.add(viewHistoryButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        ticketPanel.add(formPanel, BorderLayout.NORTH);
        ticketPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(loginPanel, "login");
        add(ticketPanel, "tickets");
    }

    // ======================== VALIDACIONES Y EVENTOS =====================
    private void setupValidations() {
        viewHistoryButton.addActionListener(e -> mostrarCronologia());
        statusCombo.addActionListener(e -> updateTicketStatus());
        loginButton.addActionListener(e -> authenticateUser());
        logoutButton.addActionListener(e -> logout());
        submitButton.addActionListener(e -> { if (validateFields()) createTicket(); });
        clearButton.addActionListener(e -> clearFields());
        registerButton.addActionListener(e -> showRegistrationDialog());
        deleteButton.addActionListener(e -> deleteTicket());
        assignButton.addActionListener(e -> assignTicket());
        editButton.addActionListener(e -> editTicket());
        chatButton.addActionListener(e -> openChatDialog());
        showPasswordCheckBox.addActionListener(e -> {
    if (showPasswordCheckBox.isSelected()) {
        passwordField.setEchoChar((char) 0); // Muestra la contraseña
    } else {
        passwordField.setEchoChar('\u2022'); // Vuelve a ocultarla (•)
    }
});

    }

    // ======================== DATABASE SETUP =============================
    private void setupDatabase() {
        try {
            DatabaseConnection.getConnection();
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    DatabaseConnection.closeConnection();
                }
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al conectar: " + e.getMessage());
        }
    }

    // ======================== AUTENTICACIÓN ==============================
    private void authenticateUser() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String sql = "SELECT id_usuario, nombre, es_administrador FROM usuarios WHERE email = ? AND password = SHA2(?, 256)";
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
                    loadTickets();
                    JOptionPane.showMessageDialog(this, "Bienvenido " + userName + (isAdmin ? " (Admin)" : ""));
                } else {
                    JOptionPane.showMessageDialog(this, "Credenciales incorrectas");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

    // ======================== ACTUALIZAR UI POR ROL =====================
    private void updateUIBasedOnRole() {
        titleField.setEditable(true);
        descriptionArea.setEditable(true);
        submitButton.setVisible(true);
        clearButton.setVisible(true);
        chatButton.setVisible(true);
        
        deleteButton.setVisible(isAdmin);
        assignButton.setVisible(isAdmin);
        editButton.setVisible(isAdmin);
        statusCombo.setEnabled(isAdmin);
        priorityCombo.setEnabled(isAdmin);
        viewHistoryButton.setVisible(isAdmin);
        statusCombo.setVisible(isAdmin);
    }

    // ======================== VALIDACIÓN DE CAMPOS =======================
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

    // ======================== CREAR TICKET ===============================
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

    // ======================== MODIFICAR TICKET ===========================
    private void editTicket() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden modificar tickets.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "ID del ticket a modificar:");
        if (input == null) return;
        try {
            int ticketId = Integer.parseInt(input);
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement sel = conn.prepareStatement(
                     "SELECT titulo, descripcion, prioridad FROM tickets WHERE id_ticket = ?")) {
                sel.setInt(1, ticketId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado.");
                        return;
                    }
                    String curTitulo = rs.getString("titulo");
                    String curDesc = rs.getString("descripcion");
                    String curPrio = rs.getString("prioridad");

                    JTextField tField = new JTextField(curTitulo);
                    JTextArea dArea = new JTextArea(curDesc, 5, 30);
                    dArea.setLineWrap(true);
                    JComboBox<String> pCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
                    pCombo.setSelectedItem(curPrio);

                    JPanel panel = new JPanel(new GridLayout(0, 1));
                    panel.add(new JLabel("Título:")); panel.add(tField);
                    panel.add(new JLabel("Descripción:")); panel.add(new JScrollPane(dArea));
                    panel.add(new JLabel("Prioridad:")); panel.add(pCombo);

                    int res = JOptionPane.showConfirmDialog(this, panel, "Modificar Ticket", JOptionPane.OK_CANCEL_OPTION);
                    if (res == JOptionPane.OK_OPTION) {
                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE tickets SET titulo = ?, descripcion = ?, prioridad = ? WHERE id_ticket = ?")) {
                            upd.setString(1, tField.getText());
                            upd.setString(2, dArea.getText());
                            upd.setString(3, (String) pCombo.getSelectedItem());
                            upd.setInt(4, ticketId);
                            upd.executeUpdate();
                        }
                        logAction("TICKET", "Ticket modificado", currentUserId);
                        loadTickets();
                        JOptionPane.showMessageDialog(this, "Ticket #" + ticketId + " modificado exitosamente.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID de ticket inválido.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al modificar ticket: " + e.getMessage());
        }
    }

    // ======================== ELIMINAR TICKET ============================
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
                JOptionPane.showMessageDialog(this, "ID de ticket inválido.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar ticket: " + e.getMessage());
            }
        }
    }

    // ======================== ASIGNAR TICKET =============================
    private void assignTicket() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden asignar tickets.");
            return;
        }
        JTextField idField = new JTextField();
        JTextField userField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("ID del Ticket:")); panel.add(idField);
        panel.add(new JLabel("ID de técnico asignado:")); panel.add(userField);
        int result = JOptionPane.showConfirmDialog(this, panel, "Asignar Ticket", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int ticketId = Integer.parseInt(idField.getText());
                int userIdToAssign = Integer.parseInt(userField.getText());
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE tickets SET id_soporte = ?, estado = 'EN PROCESO' WHERE id_ticket = ?")) {
                    pstmt.setInt(1, userIdToAssign);
                    pstmt.setInt(2, ticketId);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        logAction("TICKET", "Ticket asignado", currentUserId);
                        loadTickets();
                        JOptionPane.showMessageDialog(this, "Ticket #" + ticketId + " asignado a usuario ID " + userIdToAssign + ".");
                    } else {
                        JOptionPane.showMessageDialog(this, "Ticket o usuario no encontrado.");
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID inválido.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al asignar ticket: " + e.getMessage());
            }
        }
    }

    // ======================== CARGAR TICKETS =============================
    private void loadTickets() {
        outputArea.setText("");
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body>");
        
        String sql = isAdmin ?
            "SELECT t.*, cli.nombre AS cliente, sup.nombre AS soporte FROM tickets t " +
            "JOIN usuarios cli ON t.id_cliente = cli.id_usuario " +
            "LEFT JOIN usuarios sup ON t.id_soporte = sup.id_usuario " +
            "ORDER BY t.fecha_creacion DESC" :
            "SELECT t.*, cli.nombre AS cliente, sup.nombre AS soporte FROM tickets t " +
            "JOIN usuarios cli ON t.id_cliente = cli.id_usuario " +
            "LEFT JOIN usuarios sup ON t.id_soporte = sup.id_usuario " +
            "WHERE t.id_cliente = ? ORDER BY t.fecha_creacion DESC";
            
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!isAdmin) pstmt.setInt(1, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("estado");
                    String color;
                    switch (status.toUpperCase()) {
                        case "NUEVO": color = "red"; break;
                        case "EN PROCESO": color = "orange"; break;
                        case "COMPLETO": color = "green"; break;
                        default: color = "black";
                    }
                    
                    String soporte = rs.getString("soporte");
                    if (soporte == null) soporte = "Sin asignar";
                    
                    htmlBuilder.append(String.format(
                        "<div style='margin-bottom: 15px; border-bottom: 1px solid #ccc; padding-bottom: 10px;'>" +
                        "<b>Ticket #%d:</b> %s<br>" +
                        "<b>Cliente:</b> %s | <b>Soporte:</b> %s | <b>Prioridad:</b> %s | " +
                        "<b>Estado:</b> <span style='color:%s;'>%s</span><br>" +
                        "<b>Creado:</b> %s<br>" +
                        "<b>Descripción:</b> %s</div>",
                        rs.getInt("id_ticket"), 
                        rs.getString("titulo"), 
                        rs.getString("cliente"), 
                        soporte,
                        rs.getString("prioridad"), 
                        color, 
                        status,
                        rs.getTimestamp("fecha_creacion").toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        rs.getString("descripcion")));
                }
            }
            htmlBuilder.append("</body></html>");
            outputArea.setText(htmlBuilder.toString());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }

    // ======================== CHAT =======================================
    private void openChatDialog() {
        String input = JOptionPane.showInputDialog(this, "ID del ticket para chatear:");
        if (input == null) return;
        try {
            int ticketId = Integer.parseInt(input);
            if (!isAdmin) {
                String checkSql = "SELECT COUNT(*) FROM tickets WHERE id_ticket = ? AND id_cliente = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement chk = conn.prepareStatement(checkSql)) {
                    chk.setInt(1, ticketId);
                    chk.setInt(2, currentUserId);
                    try (ResultSet cr = chk.executeQuery()) {
                        if (cr.next() && cr.getInt(1) == 0) {
                            JOptionPane.showMessageDialog(this, "No tienes permiso para ver este ticket.");
                            return;
                        }
                    }
                }
            }
            ChatDialog cd = new ChatDialog(this, ticketId);
            cd.setVisible(true);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID inválido.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error de acceso: " + e.getMessage());
        }
    }

    // ======================== CHAT DIALOG ================================
    private class ChatDialog extends JDialog {
        private final int ticketId;
        private final JTextArea chatArea = new JTextArea(15, 40);
        private final JTextField messageField = new JTextField();
        private final JButton sendButton = new JButton("Enviar");
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        private Timer refreshTimer;


        ChatDialog(JFrame parent, int ticketId) throws SQLException {
            super(parent, "Chat del Ticket #" + ticketId, true);
            this.ticketId = ticketId;
            setLayout(new BorderLayout(5, 5));
            chatArea.setEditable(false);
            add(new JScrollPane(chatArea), BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout());
            south.add(messageField, BorderLayout.CENTER);
            south.add(sendButton, BorderLayout.EAST);
            add(south, BorderLayout.SOUTH);
            sendButton.addActionListener(e -> sendMessage());
            messageField.addActionListener(e -> sendMessage());
            loadMessages();
            // Refrescar mensajes automáticamente cada 1 segundo
            refreshTimer = new Timer(1000, e -> loadMessages());
            refreshTimer.start();
            addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (refreshTimer != null && refreshTimer.isRunning()) {
                    refreshTimer.stop();
                }
            }
        });
            pack();
            setLocationRelativeTo(parent);
        }

        private void loadMessages() {
            String sql = "SELECT m.*, u.nombre FROM mensajes m JOIN usuarios u ON m.id_usuario = u.id_usuario WHERE id_ticket = ? ORDER BY fecha_envio";
            chatArea.setText("");
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, ticketId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime f = rs.getTimestamp("fecha_envio").toLocalDateTime();
                        chatArea.append(String.format("[%s] %s: %s\n",
                            f.format(fmt), rs.getString("nombre"), rs.getString("mensaje")));
                    }
                }
            } catch (SQLException e) {
                chatArea.append("Error cargando mensajes: " + e.getMessage() + "\n");
            }
        }

        private void sendMessage() {
            String msg = messageField.getText().trim();
            if (msg.isEmpty()) return;
            String sql = "INSERT INTO mensajes(id_ticket, id_usuario, mensaje) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, ticketId);
                pstmt.setInt(2, currentUserId);
                pstmt.setString(3, msg);
                pstmt.executeUpdate();
                logAction("MENSAJE", "Nuevo mensaje", currentUserId);
                messageField.setText("");
                loadMessages();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error enviando mensaje: " + e.getMessage());
            }
        }
    }

    // ======================== AUDITORÍA ==================================
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
        JTextField regEmail = new JTextField();
        JPasswordField regPass = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre:")); panel.add(nameField);
        panel.add(new JLabel("Email:")); panel.add(regEmail);
        panel.add(new JLabel("Contraseña:")); panel.add(regPass);
        int result = JOptionPane.showConfirmDialog(this, panel, "Registro de Usuario", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (regEmail.getText().trim().isEmpty() ||
                !Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", regEmail.getText())) {
                JOptionPane.showMessageDialog(this, "Email inválido.");
                return;
            }
            if (new String(regPass.getPassword()).trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Contraseña vacía.");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO usuarios(nombre, email, password, es_administrador) VALUES (?, ?, SHA2(?, 256), false)")) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, regEmail.getText());
                pstmt.setString(3, new String(regPass.getPassword()));
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente.");
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, "El email ya está registrado.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error de registro: " + e.getMessage());
            }
        }
    }

    // ======================== LIMPIAR CAMPOS =============================
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