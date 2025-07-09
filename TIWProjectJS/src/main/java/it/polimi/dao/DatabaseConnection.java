package it.polimi.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/aste_online";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "ricky2003";

    private static final String FULL_URL = URL +
            "?useSSL=false" +
            "&serverTimezone=Europe/Rome" +
            "&characterEncoding=UTF-8" +
            "&useUnicode=true" +
            "&allowPublicKeyRetrieval=true";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ Driver MySQL caricato correttamente");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERRORE: Driver MySQL non trovato!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(FULL_URL, USERNAME, PASSWORD);
            System.out.println("✅ Connessione database stabilita");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ ERRORE connessione database: " + e.getMessage());
            throw e;
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("✅ Connessione database chiusa");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}