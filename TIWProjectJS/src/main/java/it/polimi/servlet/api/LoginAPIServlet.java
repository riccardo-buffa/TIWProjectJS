package it.polimi.servlet.api;

import it.polimi.dao.UtenteDAO;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginAPIServlet", urlPatterns = {"/api/login"})
public class LoginAPIServlet extends BaseAPIServlet {
    private UtenteDAO utenteDAO = new UtenteDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Leggi il JSON dal body della richiesta
            String jsonString = readJSONFromRequest(request);
            Map<String, Object> loginData = parseJSONToMap(jsonString);

            // Valida parametri obbligatori
            if (!validateRequiredParams(response, loginData, "username", "password")) {
                return;
            }

            String username = (String) loginData.get("username");
            String password = (String) loginData.get("password");

            logInfo("Tentativo login per: " + username);

            // Autentica l'utente
            Utente utente = utenteDAO.login(username, password);

            if (utente != null) {
                // Crea sessione
                HttpSession session = request.getSession();
                session.setAttribute("utente", utente);

                // Prepara risposta JSON
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("id", utente.getId());
                responseData.put("username", utente.getUsername());
                responseData.put("nome", utente.getNome());
                responseData.put("cognome", utente.getCognome());
                responseData.put("nomeCompleto", utente.getNomeCompleto());
                responseData.put("indirizzo", utente.getIndirizzo());

                logInfo("Login successo per: " + username);
                sendJSONResponse(response, responseData);

            } else {
                logInfo("Login fallito per: " + username);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                        "Username o password non corretti");
            }

        } catch (Exception e) {
            logError("Errore durante il login", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        sendErrorResponse(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, 
                "Metodo GET non supportato per il login");
    }
}