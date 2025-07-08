package it.polimi.dao;

import it.polimi.model.Utente;
import java.sql.*;

public class UtenteDAO {

    /**
     * Autentica un utente con username e password
     */
    public Utente login(String username, String password) {
        String sql = "SELECT * FROM utenti WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore login: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ottieni utente per ID
     */
    public Utente getById(int id) {
        String sql = "SELECT * FROM utenti WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore get utente by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Crea un nuovo utente
     */
    public boolean creaUtente(Utente utente) {
        String sql = "INSERT INTO utenti (username, password, nome, cognome, indirizzo) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, utente.getUsername());
            stmt.setString(2, utente.getPassword());
            stmt.setString(3, utente.getNome());
            stmt.setString(4, utente.getCognome());
            stmt.setString(5, utente.getIndirizzo());

            int result = stmt.executeUpdate();
            System.out.println("✅ [DAO] Utente creato: " + utente.getUsername());
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore creazione utente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Controlla se esiste già un utente con questo username
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM utenti WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore controllo username esistente: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Mappa ResultSet a oggetto Utente
     */
    private Utente mapResultSetToUtente(ResultSet rs) throws SQLException {
        Utente utente = new Utente();
        utente.setId(rs.getInt("id"));
        utente.setUsername(rs.getString("username"));
        utente.setPassword(rs.getString("password"));
        utente.setNome(rs.getString("nome"));
        utente.setCognome(rs.getString("cognome"));
        utente.setIndirizzo(rs.getString("indirizzo"));
        return utente;
    }
}