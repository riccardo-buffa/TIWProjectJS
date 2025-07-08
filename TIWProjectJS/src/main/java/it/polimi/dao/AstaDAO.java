package it.polimi.dao;

import it.polimi.model.Asta;
import it.polimi.model.Articolo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AstaDAO {
    private ArticoloDAO articoloDAO = new ArticoloDAO();

    /**
     * Crea una nuova asta
     */
    public boolean creaAsta(Asta asta, List<Integer> articoliIds) {
        String sqlAsta = "INSERT INTO aste (prezzo_iniziale, rialzo_minimo, scadenza, venditore_id) VALUES (?, ?, ?, ?)";
        String sqlAstaArticoli = "INSERT INTO asta_articoli (asta_id, articolo_id) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Inserisci l'asta
            int astaId;
            try (PreparedStatement stmtAsta = conn.prepareStatement(sqlAsta, Statement.RETURN_GENERATED_KEYS)) {
                stmtAsta.setDouble(1, asta.getPrezzoIniziale());
                stmtAsta.setInt(2, asta.getRialzoMinimo());
                stmtAsta.setTimestamp(3, Timestamp.valueOf(asta.getScadenza()));
                stmtAsta.setInt(4, asta.getVenditoreId());

                int affectedRows = stmtAsta.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creazione asta fallita");
                }

                try (ResultSet generatedKeys = stmtAsta.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        astaId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creazione asta fallita, nessun ID generato");
                    }
                }
            }

            // Inserisci gli articoli dell'asta
            try (PreparedStatement stmtArticoli = conn.prepareStatement(sqlAstaArticoli)) {
                for (Integer articoloId : articoliIds) {
                    stmtArticoli.setInt(1, astaId);
                    stmtArticoli.setInt(2, articoloId);
                    stmtArticoli.addBatch();
                }
                stmtArticoli.executeBatch();
            }

            conn.commit();
            System.out.println("✅ [DAO] Asta creata con successo: ID " + astaId);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("❌ [DAO] Errore creazione asta: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Ottieni aste per venditore
     */
    public List<Asta> getAsteByVenditore(int venditoreId, boolean chiuse) {
        String sql = "SELECT a.*, MAX(o.importo) as offerta_massima " +
                "FROM aste a LEFT JOIN offerte o ON a.id = o.asta_id " +
                "WHERE a.venditore_id = ? AND a.chiusa = ? " +
                "GROUP BY a.id ORDER BY a.scadenza ASC";

        List<Asta> aste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, venditoreId);
            stmt.setBoolean(2, chiuse);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Asta asta = mapResultSetToAsta(rs);
                    asta.setArticoli(getArticoliByAsta(asta.getId()));
                    aste.add(asta);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore get aste by venditore: " + e.getMessage());
            e.printStackTrace();
        }

        return aste;
    }

    /**
     * Cerca aste per parola chiave
     */
    public List<Asta> cercaAste(String parolaChiave) {
        String sql = "SELECT DISTINCT a.*, MAX(o.importo) as offerta_massima " +
                "FROM aste a " +
                "JOIN asta_articoli aa ON a.id = aa.asta_id " +
                "JOIN articoli art ON aa.articolo_id = art.id " +
                "LEFT JOIN offerte o ON a.id = o.asta_id " +
                "WHERE a.chiusa = FALSE AND a.scadenza > NOW() " +
                "AND (art.nome LIKE ? OR art.descrizione LIKE ?) " +
                "GROUP BY a.id " +
                "ORDER BY a.scadenza DESC";

        List<Asta> aste = new ArrayList<>();
        String searchPattern = "%" + parolaChiave + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Asta asta = mapResultSetToAsta(rs);
                    asta.setArticoli(getArticoliByAsta(asta.getId()));
                    aste.add(asta);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore cerca aste: " + e.getMessage());
            e.printStackTrace();
        }

        return aste;
    }

    /**
     * Ottieni asta per ID
     */
    public Asta getById(int id) {
        String sql = "SELECT a.*, MAX(o.importo) as offerta_massima " +
                "FROM aste a LEFT JOIN offerte o ON a.id = o.asta_id " +
                "WHERE a.id = ? GROUP BY a.id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Asta asta = mapResultSetToAsta(rs);
                    asta.setArticoli(getArticoliByAsta(id));
                    return asta;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore get asta by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Chiudi asta con debug completo e gestione errori avanzata
     */
    public boolean chiudiAsta(int astaId, Integer vincitoreId, Double prezzoFinale) {
        String sql = "UPDATE aste SET chiusa = TRUE, vincitore_id = ?, prezzo_finale = ? WHERE id = ?";

        System.out.println("💾 [DAO] ===== INIZIO CHIUSURA ASTA DATABASE =====");
        System.out.println("💾 [DAO] Parametri ricevuti:");
        System.out.println("   - Asta ID: " + astaId);
        System.out.println("   - Vincitore ID: " + vincitoreId);
        System.out.println("   - Prezzo finale: " + prezzoFinale);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Ottieni connessione
            conn = DatabaseConnection.getConnection();
            System.out.println("✅ [DAO] Connessione database ottenuta");

            // Verifica stato asta prima dell'aggiornamento
            String checkSql = "SELECT id, chiusa, vincitore_id, prezzo_finale FROM aste WHERE id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, astaId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("🔍 [DAO] Stato asta PRIMA dell'aggiornamento:");
                        System.out.println("   - ID: " + rs.getInt("id"));
                        System.out.println("   - Chiusa: " + rs.getBoolean("chiusa"));
                        System.out.println("   - Vincitore ID: " + rs.getObject("vincitore_id"));
                        System.out.println("   - Prezzo finale: " + rs.getObject("prezzo_finale"));
                    } else {
                        System.err.println("❌ [DAO] ERRORE: Asta con ID " + astaId + " non trovata!");
                        return false;
                    }
                }
            }

            // Prepara statement per l'update
            stmt = conn.prepareStatement(sql);
            System.out.println("✅ [DAO] PreparedStatement creato: " + sql);

            // Imposta i parametri
            if (vincitoreId != null && vincitoreId > 0) {
                stmt.setInt(1, vincitoreId);
                System.out.println("✅ [DAO] Parametro 1 (vincitore_id): " + vincitoreId + " (INT)");
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
                System.out.println("✅ [DAO] Parametro 1 (vincitore_id): NULL");
            }

            if (prezzoFinale != null && prezzoFinale > 0) {
                stmt.setDouble(2, prezzoFinale);
                System.out.println("✅ [DAO] Parametro 2 (prezzo_finale): " + prezzoFinale + " (DOUBLE)");
            } else {
                stmt.setNull(2, java.sql.Types.DOUBLE);
                System.out.println("✅ [DAO] Parametro 2 (prezzo_finale): NULL");
            }

            stmt.setInt(3, astaId);
            System.out.println("✅ [DAO] Parametro 3 (id): " + astaId + " (INT)");

            // Esegui l'update
            System.out.println("⚡ [DAO] Esecuzione UPDATE...");
            int rowsAffected = stmt.executeUpdate();
            System.out.println("📊 [DAO] Righe modificate: " + rowsAffected);

            if (rowsAffected > 0) {
                System.out.println("✅ [DAO] UPDATE eseguito con successo");

                // Verifica immediata del risultato
                try (PreparedStatement verifyStmt = conn.prepareStatement(checkSql)) {
                    verifyStmt.setInt(1, astaId);
                    try (ResultSet rs = verifyStmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("🔍 [DAO] Stato asta DOPO l'aggiornamento:");
                            System.out.println("   - ID: " + rs.getInt("id"));
                            System.out.println("   - Chiusa: " + rs.getBoolean("chiusa"));
                            System.out.println("   - Vincitore ID: " + rs.getObject("vincitore_id"));
                            System.out.println("   - Prezzo finale: " + rs.getObject("prezzo_finale"));

                            // Verifica coerenza
                            boolean chiusaOk = rs.getBoolean("chiusa");
                            Object vincitoreDb = rs.getObject("vincitore_id");
                            Object prezzoDb = rs.getObject("prezzo_finale");

                            boolean coerente = true;
                            if (!chiusaOk) {
                                System.err.println("❌ [DAO] ERRORE: asta non risulta chiusa!");
                                coerente = false;
                            }

                            if (vincitoreId == null && vincitoreDb != null) {
                                System.err.println("❌ [DAO] ERRORE: vincitore doveva essere NULL ma è: " + vincitoreDb);
                                coerente = false;
                            } else if (vincitoreId != null && !vincitoreId.equals(vincitoreDb)) {
                                System.err.println("❌ [DAO] ERRORE: vincitore non corrisponde. Expected: " + vincitoreId + ", Found: " + vincitoreDb);
                                coerente = false;
                            }

                            if (prezzoFinale == null && prezzoDb != null) {
                                System.err.println("❌ [DAO] ERRORE: prezzo doveva essere NULL ma è: " + prezzoDb);
                                coerente = false;
                            } else if (prezzoFinale != null && prezzoDb != null && Math.abs(prezzoFinale - ((Number)prezzoDb).doubleValue()) > 0.01) {
                                System.err.println("❌ [DAO] ERRORE: prezzo non corrisponde. Expected: " + prezzoFinale + ", Found: " + prezzoDb);
                                coerente = false;
                            }

                            if (coerente) {
                                System.out.println("✅ [DAO] Verifica coerenza: SUCCESSO");
                                return true;
                            } else {
                                System.err.println("❌ [DAO] Verifica coerenza: FALLITA");
                                return false;
                            }
                        }
                    }
                }
            } else {
                System.err.println("❌ [DAO] ERRORE: Nessuna riga modificata!");
                System.err.println("❌ [DAO] Possibili cause:");
                System.err.println("   - Asta con ID " + astaId + " non esiste");
                System.err.println("   - Asta già chiusa");
                System.err.println("   - Condizione WHERE non soddisfatta");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ [DAO] ===== ECCEZIONE SQL =====");
            System.err.println("❌ [DAO] Messaggio: " + e.getMessage());
            System.err.println("❌ [DAO] SQLState: " + e.getSQLState());
            System.err.println("❌ [DAO] ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ [DAO] ===== ECCEZIONE GENERICA =====");
            System.err.println("❌ [DAO] Messaggio: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Chiudi risorse
            if (stmt != null) {
                try {
                    stmt.close();
                    System.out.println("🔒 [DAO] PreparedStatement chiuso");
                } catch (SQLException e) {
                    System.err.println("⚠️ [DAO] Errore chiusura statement: " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("🔒 [DAO] Connessione chiusa");
                } catch (SQLException e) {
                    System.err.println("⚠️ [DAO] Errore chiusura connessione: " + e.getMessage());
                }
            }
        }

        System.out.println("💾 [DAO] ===== FINE CHIUSURA ASTA DATABASE =====");
        return false;
    }

    /**
     * Ottieni aste vinte da un utente
     */
    public List<Asta> getAsteVinte(int utenteId) {
        String sql = "SELECT a.*, MAX(o.importo) as offerta_massima " +
                "FROM aste a " +
                "LEFT JOIN offerte o ON a.id = o.asta_id " +
                "WHERE a.vincitore_id = ? AND a.chiusa = TRUE " +
                "GROUP BY a.id, a.prezzo_iniziale, a.rialzo_minimo, a.scadenza, " +
                "a.chiusa, a.venditore_id, a.vincitore_id, a.prezzo_finale, a.data_creazione " +
                "ORDER BY a.scadenza DESC";

        List<Asta> aste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Asta asta = mapResultSetToAsta(rs);

                    // Carica gli articoli per questa asta
                    asta.setArticoli(getArticoliByAsta(asta.getId()));

                    // Verifica che sia effettivamente vinta dall'utente
                    if (asta.getVincitoreId() != null && asta.getVincitoreId() == utenteId && asta.isChiusa()) {
                        aste.add(asta);
                        System.out.println("🏆 [DAO] Asta vinta caricata: ID " + asta.getId() +
                                " - Prezzo finale: €" + asta.getPrezzoFinale() +
                                " - Vincitore: " + utenteId);
                    }
                }
            }

            System.out.println("✅ [DAO] Caricate " + aste.size() + " aste vinte per utente " + utenteId);

        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore get aste vinte: " + e.getMessage());
            e.printStackTrace();
        }

        return aste;
    }

    /**
     * Ottieni articoli per un'asta
     */
    private List<Articolo> getArticoliByAsta(int astaId) {
        String sql = "SELECT a.* FROM articoli a " +
                "JOIN asta_articoli aa ON a.id = aa.articolo_id " +
                "WHERE aa.asta_id = ?";

        List<Articolo> articoli = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, astaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Articolo articolo = new Articolo();
                    articolo.setId(rs.getInt("id"));
                    articolo.setCodice(rs.getString("codice"));
                    articolo.setNome(rs.getString("nome"));
                    articolo.setDescrizione(rs.getString("descrizione"));
                    articolo.setImmagine(rs.getString("immagine"));
                    articolo.setPrezzo(rs.getDouble("prezzo"));
                    articolo.setProprietarioId(rs.getInt("proprietario_id"));
                    articolo.setVenduto(rs.getBoolean("venduto"));
                    articoli.add(articolo);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ [DAO] Errore get articoli by asta: " + e.getMessage());
            e.printStackTrace();
        }

        return articoli;
    }

    /**
     * Mappa ResultSet a Asta
     */
    private Asta mapResultSetToAsta(ResultSet rs) throws SQLException {
        Asta asta = new Asta();
        asta.setId(rs.getInt("id"));
        asta.setPrezzoIniziale(rs.getDouble("prezzo_iniziale"));
        asta.setRialzoMinimo(rs.getInt("rialzo_minimo"));
        asta.setScadenza(rs.getTimestamp("scadenza").toLocalDateTime());
        asta.setChiusa(rs.getBoolean("chiusa"));
        asta.setVenditoreId(rs.getInt("venditore_id"));

        // Gestione vincitore (può essere null)
        int vincitoreId = rs.getInt("vincitore_id");
        if (!rs.wasNull()) {
            asta.setVincitoreId(vincitoreId);
        }

        // Gestione prezzo finale (può essere null)
        double prezzoFinale = rs.getDouble("prezzo_finale");
        if (!rs.wasNull()) {
            asta.setPrezzoFinale(prezzoFinale);
        }

        // Gestione offerta massima (può essere null)
        double offertaMassima = rs.getDouble("offerta_massima");
        if (!rs.wasNull()) {
            asta.setOffertaMassima(offertaMassima);
        }
        // Se null, getOffertaMassima() ritornerà automaticamente il prezzo iniziale

        return asta;
    }
}