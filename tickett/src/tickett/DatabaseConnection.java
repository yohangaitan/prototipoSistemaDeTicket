package tickett; // ¡CORREGIDO! El paquete ahora es 'tickett' para que coincida con la ubicación de la carpeta

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection { // ¡CORREGIDO! El nombre de la clase ahora es 'DatabaseConnection'

    // --- Credenciales de la base de datos ---
    // ¡ATENCIÓN! En un entorno de producción, nunca coloques las credenciales directamente en el código.
    // Esto es por simplicidad y petición tuya.
    private static final String URL = "jdbc:mysql://localhost:3306/teknosa";
    private static final String USER = "root"; // O el usuario que estés usando en tu MySQL
    private static final String PASSWORD = "root"; // O la contraseña que estés usando en tu MySQL

    private static Connection connection; // Esta conexión es estática y única

    /**
     * Obtiene una conexión a la base de datos. Si no hay una conexión activa o está cerrada,
     * intenta establecer una nueva.
     * @return Una conexión JDBC a la base de datos.
     * @throws SQLException Si ocurre un error al conectar con la base de datos o si el driver no se encuentra.
     */
    public static Connection getConnection() throws SQLException {
        // Si la conexión es nula (nunca se ha establecido) o si está cerrada, intenta crear una nueva
        if (connection == null || connection.isClosed()) {
            try {
                // Carga el driver JDBC de MySQL. Esto es necesario para que Java sepa cómo conectarse a MySQL.
                // Asegúrate de que el archivo JAR 'mysql-connector-j-9.3.0.jar' (o la versión que uses)
                // esté añadido a las librerías de tu proyecto en IntelliJ.
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Establece la conexión con la base de datos usando la URL, usuario y contraseña
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                // Si el driver no se encuentra (ej. el JAR no está en las librerías), lanza una excepción
                throw new SQLException("Driver JDBC de MySQL no encontrado. Asegúrate de que el JAR del conector esté en las librerías del proyecto.", e);
            }
        }
        return connection; // Devuelve la conexión existente o la recién creada
    }

    /**
     * Cierra la conexión a la base de datos si está abierta.
     * Este método es importante para liberar recursos cuando la aplicación se cierra.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexión a la base de datos cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
            // Para depuración, puedes imprimir el stack trace:
            // e.printStackTrace();
        }
    }
}