package ec.fraud.web.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    // XAMPP defaults (adjust if your settings differ)
    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/project?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        // MySQL Connector/J 8 uses com.mysql.cj.jdbc.Driver
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
