package tickett;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

public class TeknosaTicketSystem extends JFrame {

    // Componentes UI y Variables de Estado
    private JButton createUserButton; // al inicio junto a otros botones
    private JTextField titleField, emailField;
    private JPasswordField passwordField;
    private JTextArea descriptionArea;
    private JTextPane outputArea;
    private JComboBox<String> priorityCombo, statusCombo;
    private JButton submitButton, clearButton, loginButton, registerButton,
                   deleteButton, assignButton, editButton, chatButton,
                   logoutButton, viewHistoryButton;
    private JLabel priorityLabel;
    private JPanel loginPanel, ticketPanel;
    private CardLayout cardLayout;
    private JCheckBox showPasswordCheckBox;

    private boolean isAdmin = false;
    private boolean esTecnico = false;
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
        updateUIBasedOnRole();
    }

    private void initComponents() {
        createUserButton = new JButton("Crear Usuario");
        titleField = new JTextField(30);
        descriptionArea = new JTextArea(8, 30);
        descriptionArea.setLineWrap(true);
        priorityCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
        priorityLabel = new JLabel("Prioridad:");
        statusCombo = new JComboBox<>(new String[]{"NUEVO", "EN PROCESO", "COMPLETO"});

        submitButton = new JButton("Crear Ticket");
        clearButton = new JButton("Limpiar");
        deleteButton = new JButton("Eliminar Ticket");
        assignButton = new JButton("Asignar Ticket");
        editButton = new JButton("Modificar Ticket");
        chatButton = new JButton("Chat");
        logoutButton = new JButton("Cerrar Sesión");
        viewHistoryButton = new JButton("Ver Cronología");

        outputArea = new JTextPane();
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
        formPanel.add(priorityLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(priorityCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusCombo, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(createUserButton);
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

    private void setupValidations() {
        createUserButton.addActionListener(e -> showUserCreationDialog());
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
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('\u2022');
            }
        });
    }
    
    // crear usuarios dentro de admin
    private void showUserCreationDialog() {
    JTextField nameField = new JTextField();
    JTextField lastNameField = new JTextField();
    JTextField emailField = new JTextField();
    JPasswordField passwordField = new JPasswordField();

    String[] roles = {"Cliente", "Técnico", "Administrador"};
    JComboBox<String> roleComboBox = new JComboBox<>(roles);

    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("Nombre:")); panel.add(nameField);
    panel.add(new JLabel("Apellido:")); panel.add(lastNameField);
    panel.add(new JLabel("Email:")); panel.add(emailField);
    panel.add(new JLabel("Contraseña:")); panel.add(passwordField);
    panel.add(new JLabel("Rol:")); panel.add(roleComboBox);

    int result = JOptionPane.showConfirmDialog(this, panel, "Crear Nuevo Usuario", JOptionPane.OK_CANCEL_OPTION);
    if (result == JOptionPane.OK_OPTION) {
        String nombre = nameField.getText().trim();
        String apellido = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String rol = (String) roleComboBox.getSelectedItem();

        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.");
            return;
        }

        boolean esAdmin = rol.equals("Administrador");
        boolean esTecnico = rol.equals("Técnico");

        String sql = "INSERT INTO usuarios (nombre, apellido, email, password, es_administrador, es_tecnico) VALUES (?, ?, ?, SHA2(?, 256), ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, apellido);
            pstmt.setString(3, email);
            pstmt.setString(4, password);
            pstmt.setBoolean(5, esAdmin);
            pstmt.setBoolean(6, esTecnico);
            pstmt.executeUpdate();
            logAction("USUARIO", "Nuevo usuario creado por administrador", currentUserId);
            JOptionPane.showMessageDialog(this, "Usuario creado exitosamente.");
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this, "El email ya está registrado.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }
}

    
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

    private void logout() {
        if (statusCombo.getActionListeners().length > 0) {
            statusCombo.removeActionListener(statusCombo.getActionListeners()[0]);
        }
        clearFields();
        emailField.setText("");
        passwordField.setText("");
        currentUserId = 0;
        isAdmin = false;
        esTecnico = false;
        cardLayout.show(getContentPane(), "login");
        outputArea.setText("");
        updateUIBasedOnRole();
        statusCombo.addActionListener(e -> updateTicketStatus());
    }

    private boolean isSoporte() {
        return esTecnico;
    }
    
    private void authenticateUser() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String sql = "SELECT id_usuario, nombre, es_administrador, es_tecnico FROM usuarios WHERE email = ? AND password = SHA2(?, 256)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentUserId = rs.getInt("id_usuario");
                    isAdmin = rs.getBoolean("es_administrador");
                    esTecnico = rs.getBoolean("es_tecnico");
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
    
    private void updateUIBasedOnRole() {
        createUserButton.setVisible(isAdmin);
        boolean loggedIn = currentUserId != 0; 
        boolean isPrivileged = isAdmin || isSoporte(); 

        titleField.setEditable(loggedIn); 
        descriptionArea.setEditable(loggedIn); 
        submitButton.setVisible(loggedIn); 
        clearButton.setVisible(loggedIn); 
        chatButton.setVisible(loggedIn); 
        logoutButton.setVisible(loggedIn); 

        deleteButton.setVisible(isAdmin); 
        assignButton.setVisible(isAdmin); 
        
        editButton.setVisible(isPrivileged); 
        statusCombo.setEnabled(isPrivileged); 
        
        viewHistoryButton.setVisible(loggedIn);
        
        priorityLabel.setVisible(isPrivileged); 
        priorityCombo.setVisible(isPrivileged); 
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
    
    private void editTicket() {
        if (!isAdmin && !isSoporte()) {
            JOptionPane.showMessageDialog(this, "Solo administradores o técnicos pueden modificar tickets.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "ID del ticket a modificar:");
        if (input == null) return;
        try {
            int ticketId = Integer.parseInt(input);
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement sel = conn.prepareStatement(
                         "SELECT titulo, descripcion, prioridad, estado FROM tickets WHERE id_ticket = ?")) {
                sel.setInt(1, ticketId);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado.");
                        return;
                    }
                    String curTitulo = rs.getString("titulo");
                    String curDesc = rs.getString("descripcion");
                    String curPrio = rs.getString("prioridad");
                    String curEstado = rs.getString("estado");
                    
                    JTextField tField = new JTextField(curTitulo);
                    JTextArea dArea = new JTextArea(curDesc, 5, 30);
                    dArea.setLineWrap(true);
                    JComboBox<String> pCombo = new JComboBox<>(new String[]{"ALTA", "MEDIA", "BAJA"});
                    pCombo.setSelectedItem(curPrio);
                    JComboBox<String> sCombo = new JComboBox<>(new String[]{"NUEVO", "EN PROCESO", "COMPLETO"});
                    sCombo.setSelectedItem(curEstado);
                    
                    JPanel panel = new JPanel(new GridLayout(0, 1));
                    if (isAdmin) {
                        panel.add(new JLabel("Título:"));
                        panel.add(tField);
                        panel.add(new JLabel("Descripción:")); panel.add(new JScrollPane(dArea));
                    }
                    panel.add(new JLabel("Prioridad:"));
                    panel.add(pCombo);
                    panel.add(new JLabel("Estado:")); panel.add(sCombo);

                    int res = JOptionPane.showConfirmDialog(this, panel, "Modificar Ticket", JOptionPane.OK_CANCEL_OPTION);
                    if (res == JOptionPane.OK_OPTION) {
                        String nuevaPrioridad = (String) pCombo.getSelectedItem();
                        String nuevoEstado = (String) sCombo.getSelectedItem();
                        if (isAdmin) {
                            try (PreparedStatement upd = conn.prepareStatement(
                                    "UPDATE tickets SET titulo = ?, descripcion = ?, prioridad = ?, estado = ? WHERE id_ticket = ?")) {
                                upd.setString(1, tField.getText());
                                upd.setString(2, dArea.getText());
                                upd.setString(3, nuevaPrioridad);
                                upd.setString(4, nuevoEstado);
                                upd.setInt(5, ticketId);
                                upd.executeUpdate();
                            }
                        } else if (isSoporte()) {
                            try (PreparedStatement upd = conn.prepareStatement(
                                    "UPDATE tickets SET prioridad = ?, estado = ? WHERE id_ticket = ?")) {
                                upd.setString(1, nuevaPrioridad);
                                upd.setString(2, nuevoEstado);
                                upd.setInt(3, ticketId);
                                upd.executeUpdate();
                            }
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

    private void updateTicketStatus() {
        if (!isAdmin && !isSoporte()) return;
        String input = JOptionPane.showInputDialog(this, "ID del ticket para cambiar estado:");
        if (input != null) {
            try {
                int ticketId = Integer.parseInt(input);
                String estadoAnterior = "";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement sel = conn.prepareStatement( "SELECT estado FROM tickets WHERE id_ticket = ?")) {
                    sel.setInt(1, ticketId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) { estadoAnterior = rs.getString("estado");
                        }
                        else { JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado.");
                        return; }
                    }
                }
                String nuevoEstado = (String) statusCombo.getSelectedItem();
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement update = conn.prepareStatement("UPDATE tickets SET estado = ? WHERE id_ticket = ?")) {
                    update.setString(1, nuevoEstado);
                    update.setInt(2, ticketId); update.executeUpdate();
                }
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement crono = conn.prepareStatement("INSERT INTO cronologia(id_ticket, estado_anterior, estado_nuevo, id_usuario) VALUES (?, ?, ?, ?)")) {
                    crono.setInt(1, ticketId);
                    crono.setString(2, estadoAnterior); crono.setString(3, nuevoEstado); crono.setInt(4, currentUserId); crono.executeUpdate();
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
    
    private void loadTickets() {
        outputArea.setText("");
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body style='font-family: Arial, sans-serif;'>");

        String sql;
        if (isAdmin) {
            sql = "SELECT t.*, CONCAT(cli.nombre, ' ', cli.apellido) AS cliente, CONCAT(sup.nombre, ' ', sup.apellido) AS soporte " +
                  "FROM tickets t " +
                  "JOIN usuarios cli ON t.id_cliente = cli.id_usuario " +
                  "LEFT JOIN usuarios sup ON t.id_soporte = sup.id_usuario " +
                  "ORDER BY t.fecha_creacion DESC";
        } else if (isSoporte()) {
            sql = "SELECT t.*, CONCAT(cli.nombre, ' ', cli.apellido) AS cliente, CONCAT(sup.nombre, ' ', sup.apellido) AS soporte " +
                  "FROM tickets t " +
                  "JOIN usuarios cli ON t.id_cliente = cli.id_usuario " +
                  "LEFT JOIN usuarios sup ON t.id_soporte = sup.id_usuario " +
                  "WHERE t.id_soporte = ? " +
                  "ORDER BY t.fecha_creacion DESC";
        } else {
            sql = "SELECT t.*, CONCAT(cli.nombre, ' ', cli.apellido) AS cliente, CONCAT(sup.nombre, ' ', sup.apellido) AS soporte " +
                  "FROM tickets t " +
                  "JOIN usuarios cli ON t.id_cliente = cli.id_usuario " +
                  "LEFT JOIN usuarios sup ON t.id_soporte = sup.id_usuario " +
                  "WHERE t.id_cliente = ? " +
                  "ORDER BY t.fecha_creacion DESC";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (!isAdmin) { pstmt.setInt(1, currentUserId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("estado");
                    String color;
                    switch (status.toUpperCase()) {
                        case "NUEVO": color = "#d9534f"; break;
                        case "EN PROCESO": color = "#f0ad4e"; break;
                        case "COMPLETO": color = "#5cb85c"; break;
                        default: color = "black";
                    }
                    String soporte = rs.getString("soporte");
                    if (soporte == null) soporte = "Sin asignar";

                    String priorityString = "";
                    if (isAdmin || isSoporte()) {
                        priorityString = String.format("<b>Prioridad:</b> %s | ", rs.getString("prioridad"));
                    }

                    htmlBuilder.append(String.format(
                        "<div style='margin-bottom: 15px; border-bottom: 1px solid #ccc; padding-bottom: 10px;'>" +
                        "<b>Ticket #%d:</b> %s<br>" +
                        "<b>Cliente:</b> %s | <b>Soporte:</b> %s | %s" +
                        "<b>Estado:</b> <span style='color:%s; font-weight:bold;'>%s</span><br>" +
                        "<b>Creado:</b> %s<br>" +
                        "<b>Descripción:</b> <div style='padding-left:10px;'>%s</div></div>",
                        rs.getInt("id_ticket"),
                        rs.getString("titulo"),
                        rs.getString("cliente"),
                        soporte,
                        priorityString,
                        color,
                        status,
                        rs.getTimestamp("fecha_creacion").toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        rs.getString("descripcion").replaceAll("\n", "<br>")
                    ));
                }
            }
            htmlBuilder.append("</body></html>");
            outputArea.setText(htmlBuilder.toString());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL al cargar tickets: " + e.getMessage());
        }
    }
    
    private void assignTicket() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Solo los administradores pueden asignar tickets.");
            return;
        }
        Map<String, Integer> technicians = getTechnicians();
        if (technicians.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay técnicos disponibles para asignar.");
            return;
        }
    
        JTextField idField = new JTextField();
        JComboBox<String> techComboBox = new JComboBox<>(new Vector<>(technicians.keySet()));
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("ID del Ticket:")); panel.add(idField);
        panel.add(new JLabel("Asignar al técnico:")); panel.add(techComboBox);
    
        int result = JOptionPane.showConfirmDialog(this, panel, "Asignar Ticket", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int ticketId = Integer.parseInt(idField.getText().trim());
                String selectedTechName = (String) techComboBox.getSelectedItem();
                int userIdToAssign = technicians.get(selectedTechName);
    
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE tickets SET id_soporte = ?, estado = 'EN PROCESO' WHERE id_ticket = ?")) {
                    pstmt.setInt(1, userIdToAssign);
                    pstmt.setInt(2, ticketId);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        logAction("TICKET", "Ticket asignado", currentUserId);
                        loadTickets();
                        JOptionPane.showMessageDialog(this, "Ticket #" + ticketId + " asignado al técnico " + selectedTechName + ".");
                    } else {
                        JOptionPane.showMessageDialog(this, "Ticket con ID " + ticketId + " no encontrado.");
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID de ticket inválido. Debe ser un número.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error SQL al asignar ticket: " + e.getMessage());
            }
        }
    }

    private Map<String, Integer> getTechnicians() {
        Map<String, Integer> technicians = new HashMap<>();
        String sql = "SELECT id_usuario, nombre FROM usuarios WHERE es_tecnico = true";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                technicians.put(rs.getString("nombre"), rs.getInt("id_usuario"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar la lista de técnicos: " + e.getMessage());
        }
        return technicians;
    }

    // =================== MÉTODOS MODIFICADOS PARA CRONOLOGÍA ====================
    private void mostrarCronologia() {
        String input = JOptionPane.showInputDialog(this, "ID del ticket para ver su cronología:"); 
        if (input != null && !input.trim().isEmpty()) { 
            try {
                int ticketId = Integer.parseInt(input);
                
                // Verificar permisos antes de mostrar
                if (!tienePermisoVerTicket(currentUserId, ticketId)) {
                    JOptionPane.showMessageDialog(this, "No tienes permiso para ver este ticket.");
                    return;
                }
                
                boolean canEdit = isAdmin || esTecnico;
                new ChronologyDialog(this, ticketId, currentUserId, canEdit).setVisible(true);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID de ticket inválido. Por favor, introduzca un número."); 
            }
        }
    }

    private boolean tienePermisoVerTicket(int userId, int ticketId) {
        String sql = "SELECT id_cliente, id_soporte FROM tickets WHERE id_ticket = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int idCliente = rs.getInt("id_cliente");
                int idSoporte = rs.getInt("id_soporte");
                
                // El usuario puede ver el ticket si:
                // 1. Es el cliente que creó el ticket
                // 2. Es el técnico asignado
                // 3. Es administrador
                return idCliente == userId || 
                       (esTecnico && idSoporte == userId) || 
                       isAdmin;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al verificar permisos: " + e.getMessage());
        }
        return false;
    }

    private void openChatDialog() {
        String input = JOptionPane.showInputDialog(this, "ID del ticket para chatear:");
        if (input == null) return;
        try {
            int ticketId = Integer.parseInt(input);
            String checkSql = "SELECT id_cliente, id_soporte FROM tickets WHERE id_ticket = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement chk = conn.prepareStatement(checkSql)) {
                chk.setInt(1, ticketId);
                try (ResultSet cr = chk.executeQuery()) {
                    if (cr.next()) {
                        int clienteId = cr.getInt("id_cliente");
                        int soporteId = cr.getInt("id_soporte");

                        boolean esPropietario = currentUserId == clienteId;
                        boolean esSoporteAsignado = esTecnico && currentUserId == soporteId;
                        if (!isAdmin && !(esPropietario || esSoporteAsignado)) {
                            JOptionPane.showMessageDialog(this, "No tienes permiso para ver este ticket.");
                            return;
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "El ticket no existe.");
                        return;
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
    
    private void logAction(String entity, String action, int userId) {
        String sql = "INSERT INTO auditoria (accion, entidad, id_usuario, detalles, ip_origen) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        JTextField lastNameField = new JTextField();
        JTextField regEmail = new JTextField();
        JPasswordField regPass = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre:")); panel.add(nameField);
        panel.add(new JLabel("Apellido:")); panel.add(lastNameField);
        panel.add(new JLabel("Email:")); panel.add(regEmail);
        panel.add(new JLabel("Contraseña:")); panel.add(regPass);
        int result = JOptionPane.showConfirmDialog(this, panel, "Registro de Usuario", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (regEmail.getText().trim().isEmpty() || !Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", regEmail.getText())) {
                JOptionPane.showMessageDialog(this, "Email inválido.");
                return;
            }
            if (new String(regPass.getPassword()).trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Contraseña vacía.");
                return;
            }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO usuarios(nombre, apellido, email, password, es_administrador) VALUES (?, ?, ?, SHA2(?, 256), false)")) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, lastNameField.getText());
                pstmt.setString(3, regEmail.getText());
                pstmt.setString(4, new String(regPass.getPassword()));
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Cuenta creada exitosamente.");
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, "El email ya está registrado.");
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
    
    // ==================== CLASES INTERNAS ====================
    private class ValidationFilter extends DocumentFilter {
        private final Pattern pattern = Pattern.compile("[^a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ.,;:()@\\- ]");
        @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && !pattern.matcher(string).find()) { super.insertString(fb, offset, string, attr);
            }
            else { JOptionPane.showMessageDialog(null, "Caracteres no permitidos.");
            }
        }
        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || !pattern.matcher(text).find()) { super.replace(fb, offset, length, text, attrs);
            }
            else { JOptionPane.showMessageDialog(null, "Caracteres no permitidos.");
            }
        }
    }
    
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
            refreshTimer = new Timer(1000, e -> loadMessages());
            refreshTimer.start();
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { if (refreshTimer != null && refreshTimer.isRunning()) { refreshTimer.stop(); } }
            });
            pack(); setLocationRelativeTo(parent);
        }
        
        private void loadMessages() {
            String sql = "SELECT m.*, u.nombre FROM mensajes m JOIN usuarios u ON m.id_usuario = u.id_usuario WHERE id_ticket = ? ORDER BY fecha_envio";
            chatArea.setText("");
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, ticketId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime f = rs.getTimestamp("fecha_envio").toLocalDateTime();
                        chatArea.append(String.format("[%s] %s: %s\n", f.format(fmt), rs.getString("nombre"), rs.getString("mensaje")));
                    }
                }
            } catch (SQLException e) { chatArea.append("Error cargando mensajes: " + e.getMessage() + "\n");
            }
        }
        
        private void sendMessage() {
            String msg = messageField.getText().trim();
            if (msg.isEmpty()) return;
            String sql = "INSERT INTO mensajes(id_ticket, id_usuario, mensaje) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, ticketId);
                pstmt.setInt(2, currentUserId); pstmt.setString(3, msg); pstmt.executeUpdate();
                logAction("MENSAJE", "Nuevo mensaje", currentUserId); messageField.setText(""); loadMessages();
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error enviando mensaje: " + e.getMessage());
            }
        }
    }

    private class ChronologyDialog extends JDialog {
        private final int ticketId;
        private final int currentUserId;
        private final boolean isPrivilegedUser;
        private JList<ChronologyEntry> historyList;
        private DefaultListModel<ChronologyEntry> listModel;
        private JTextArea commentArea;
        private JButton addButton, editButton, closeButton;
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ChronologyDialog(JFrame parent, int ticketId, int userId, boolean isPrivileged) {
            super(parent, "Bitácora del Ticket #" + ticketId, true);
            this.ticketId = ticketId;
            this.currentUserId = userId;
            this.isPrivilegedUser = isPrivileged;

            // Verificación adicional de seguridad
            if (!tienePermisoVerTicket(userId, ticketId)) {
                JOptionPane.showMessageDialog(parent, "Acceso no autorizado a este ticket.");
                dispose();
                return;
            }

            initComponents();
            layoutComponents();
            addListeners();
            loadHistory();

            setSize(600, 500);
            setLocationRelativeTo(parent);
        }

        private void initComponents() {
            listModel = new DefaultListModel<>();
            historyList = new JList<>(listModel);
            historyList.setCellRenderer(new ChronologyRenderer());
            
            if (isPrivilegedUser) {
                commentArea = new JTextArea(4, 30);
                commentArea.setLineWrap(true);
                commentArea.setWrapStyleWord(true);
                addButton = new JButton("Agregar Entrada");
                editButton = new JButton("Editar Selección");
                editButton.setEnabled(false);
            }
            
            closeButton = new JButton("Cerrar");
        }
        
        private void layoutComponents() {
            setLayout(new BorderLayout(10, 10));
            add(new JScrollPane(historyList), BorderLayout.CENTER);
            
            JPanel southContainer = new JPanel(new BorderLayout(5, 5));
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            
            if (isPrivilegedUser) {
                southContainer.add(new JScrollPane(commentArea), BorderLayout.CENTER);
                southContainer.setBorder(BorderFactory.createTitledBorder("Nueva Entrada de Bitácora"));
                buttonPanel.add(addButton);
                buttonPanel.add(editButton);
            }
            
            buttonPanel.add(closeButton);
            southContainer.add(buttonPanel, BorderLayout.SOUTH);
            
            add(southContainer, BorderLayout.SOUTH);
        }
        
        private void addListeners() {
            closeButton.addActionListener(e -> dispose());

            if (isPrivilegedUser) {
                addButton.addActionListener(e -> addEntry());

                historyList.addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        ChronologyEntry selected = historyList.getSelectedValue();
                        editButton.setEnabled(selected != null && selected.getUserId() == currentUserId);
                    }
                });
                editButton.addActionListener(e -> editEntry());
            }
        }

        private void loadHistory() {
            listModel.clear();
            String sql = "SELECT c.*, u.nombre FROM cronologia c JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE c.id_ticket = ? ORDER BY c.fecha ASC";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, ticketId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    listModel.addElement(new ChronologyEntry(
                        rs.getInt("id"),
                        rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("estado_anterior"),
                        rs.getString("estado_nuevo"),
                        rs.getString("comentario"),
                        rs.getString("nombre"),
                        rs.getInt("id_usuario")
                    ));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al cargar la cronología: " + e.getMessage());
            }
        }
        
        private void addEntry() {
            String comment = commentArea.getText().trim();
            if (comment.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El comentario no puede estar vacío.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String sql = "INSERT INTO cronologia (id_ticket, id_usuario, comentario) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, ticketId);
                pstmt.setInt(2, currentUserId);
                pstmt.setString(3, comment);
                pstmt.executeUpdate();
                commentArea.setText("");
                loadHistory();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar la entrada: " + e.getMessage());
            }
        }

        private void editEntry() {
            ChronologyEntry selected = historyList.getSelectedValue();
            if (selected == null || selected.getUserId() != currentUserId) {
                JOptionPane.showMessageDialog(this, "Seleccione una entrada propia para editar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String currentComment = selected.getComment() == null ? "" : selected.getComment();
            String newComment = JOptionPane.showInputDialog(this, "Edite su comentario:", currentComment);
            if (newComment != null && !newComment.trim().equals(currentComment)) {
                String sql = "UPDATE cronologia SET comentario = ? WHERE id = ?";
                try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, newComment.trim());
                    pstmt.setInt(2, selected.getId());
                    pstmt.executeUpdate();
                    loadHistory();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Error al actualizar la entrada: " + e.getMessage());
                }
            }
        }

        private class ChronologyEntry {
            private int id, userId;
            private LocalDateTime date;
            private String oldState, newState, comment, userName;

            public ChronologyEntry(int id, LocalDateTime date, String o, String n, String c, String uName, int uId) {
                this.id = id;
                this.date = date; this.oldState = o; this.newState = n; 
                this.comment = c; this.userName = uName; this.userId = uId;
            }
            public int getId() { return id;
            }
            public int getUserId() { return userId;
            }
            public String getComment() { return comment;
            }
            
            public String getFormattedText() {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("<html><body style='width: 400px;'><b>[%s] %s:</b><br>", date.format(fmt), userName));
                if (oldState != null && newState != null) {
                    sb.append(String.format("Cambio de estado: <i>%s</i> ➝ <b>%s</b><br>", oldState, newState));
                }
                if (comment != null && !comment.isEmpty()) {
                    sb.append("Comentario: ").append(comment.replaceAll("\n", "<br>"));
                }
                sb.append("</body></html>");
                return sb.toString();
            }
        }

        private class ChronologyRenderer extends DefaultListCellRenderer {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val, int i, boolean sel, boolean foc) {
                JLabel label = (JLabel) super.getListCellRendererComponent(l, val, i, sel, foc);
                if (val instanceof ChronologyEntry) {
                    label.setText(((ChronologyEntry) val).getFormattedText());
                    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                }
                return label;
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new TeknosaTicketSystem().setVisible(true));
    }
}