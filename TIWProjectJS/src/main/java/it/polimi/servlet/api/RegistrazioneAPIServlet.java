package it.polimi.servlet.api;

import it.polimi.dao.UtenteDAO;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

//@WebServlet(name = "RegistrazioneAPIServlet", urlPatterns = {"/api/register"})
public class RegistrazioneAPIServlet extends BaseAPIServlet {
    private UtenteDAO utenteDAO = new UtenteDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Leggi il JSON dal body della richiesta
            String jsonString = readJSONFromRequest(request);
            Map<String, Object> userData = parseJSONToMap(jsonString);

            // Valida parametri obbligatori
            if (!validateRequiredParams(response, userData, "username", "password", "nome", "cognome", "indirizzo")) {
                return;
            }

            String username = (String) userData.get("username");
            String password = (String) userData.get("password");
            String nome = (String) userData.get("nome");
            String cognome = (String) userData.get("cognome");
            String indirizzo = (String) userData.get("indirizzo");

            logInfo("Tentativo registrazione per: " + username);

            // Validazione dati server-side
            String erroreValidazione = validaDatiRegistrazione(username, password, nome, cognome, indirizzo);
            if (erroreValidazione != null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, erroreValidazione);
                return;
            }

            // Controlla se username già esiste
            if (utenteDAO.existsByUsername(username)) {
                logInfo("Username già esistente: " + username);
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT,
                        "Username '" + username + "' già esistente. Scegline un altro!");
                return;
            }

            // Crea nuovo utente
            Utente nuovoUtente = new Utente(username.trim(), password, nome.trim(), cognome.trim(), indirizzo.trim());

            if (utenteDAO.creaUtente(nuovoUtente)) {
                logInfo("Registrazione completata per: " + username);

                // Prepara risposta di successo (senza creare sessione automaticamente)
                Map<String, Object> responseData = Map.of(
                        "message", "Registrazione completata con successo!",
                        "username", username,
                        "redirectToLogin", true
                );

                sendJSONResponse(response, responseData);

            } else {
                logError("Errore database durante registrazione: " + username, null);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Errore interno del server. Riprova più tardi.");
            }

        } catch (Exception e) {
            logError("Errore durante la registrazione", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore interno del server");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Metodo GET non supportato per la registrazione");
    }

    /**
     * Valida i dati di registrazione
     */
    private String validaDatiRegistrazione(String username, String password, String nome, String cognome, String indirizzo) {

        // Controlla lunghezze e formato username
        if (username.trim().length() < 3) {
            return "Username deve essere di almeno 3 caratteri";
        }
        if (username.trim().length() > 50) {
            return "Username troppo lungo (massimo 50 caratteri)";
        }
        if (!username.matches("^[a-zA-Z0-9._]+$")) {
            return "Username può contenere solo lettere, numeri, punti e underscore";
        }

        // Controlla password
        if (password.length() < 6) {
            return "Password deve essere di almeno 6 caratteri";
        }
        if (password.length() > 100) {
            return "Password troppo lunga (massimo 100 caratteri)";
        }

        // Controlla nome
        if (nome.trim().length() == 0) {
            return "Nome è obbligatorio";
        }
        if (nome.trim().length() > 50) {
            return "Nome troppo lungo (massimo 50 caratteri)";
        }

        // Controlla cognome
        if (cognome.trim().length() == 0) {
            return "Cognome è obbligatorio";
        }
        if (cognome.trim().length() > 50) {
            return "Cognome troppo lungo (massimo 50 caratteri)";
        }

        // Controlla indirizzo
        if (indirizzo.trim().length() == 0) {
            return "Indirizzo è obbligatorio";
        }
        if (indirizzo.trim().length() > 200) {
            return "Indirizzo troppo lungo (massimo 200 caratteri)";
        }

        return null; // Tutto OK
    }
}