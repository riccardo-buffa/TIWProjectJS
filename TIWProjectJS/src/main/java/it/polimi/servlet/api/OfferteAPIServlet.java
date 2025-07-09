package it.polimi.servlet.api;

import it.polimi.dao.AstaDAO;
import it.polimi.dao.OffertaDAO;
import it.polimi.model.Asta;
import it.polimi.model.Offerta;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "OfferteAPIServlet", urlPatterns = {"/api/offerte", "/api/offerte/*"})
public class OfferteAPIServlet extends BaseAPIServlet {
    private OffertaDAO offertaDAO = new OffertaDAO();
    private AstaDAO astaDAO = new AstaDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verifica autenticazione
        Utente utente = requireAuthentication(request, response);
        if (utente == null) return;

        try {
            // Leggi dati dal JSON
            String jsonString = readJSONFromRequest(request);
            Map<String, Object> offertaData = parseJSONToMap(jsonString);

            // Valida parametri obbligatori
            if (!validateRequiredParams(response, offertaData, "astaId", "importo")) {
                return;
            }

            int astaId = getIntFromDouble(offertaData.get("astaId"));
            double importo = ((Number) offertaData.get("importo")).doubleValue();

            logInfo("Creazione offerta di €" + importo + " per asta " + astaId + 
                    " da utente: " + utente.getUsername());

            // Verifica validità dell'asta
            Asta asta = astaDAO.getById(astaId);
            if (asta == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Asta non trovata");
                return;
            }

            if (asta.isChiusa()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "L'asta è già chiusa");
                return;
            }

            if (asta.isScaduta()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "L'asta è scaduta");
                return;
            }

            // Verifica che l'utente non sia il venditore
            if (asta.getVenditoreId() == utente.getId()) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                        "Non puoi fare offerte sulle tue aste");
                return;
            }

            // Verifica importo minimo
            double offertaMassima = asta.getOffertaMassima();
            double minimaRichiesta = offertaMassima + asta.getRialzoMinimo();

            if (importo < minimaRichiesta) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "L'offerta deve essere almeno €" + String.format("%.2f", minimaRichiesta));
                return;
            }

            // Crea l'offerta
            Offerta offerta = new Offerta(astaId, utente.getId(), importo);

            if (offertaDAO.creaOfferta(offerta)) {
                logInfo("Offerta creata con successo");
                sendSuccessResponse(response, "Offerta registrata con successo", offerta);
            } else {
                logError("Errore creazione offerta", null);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Errore nella registrazione dell'offerta");
            }

        } catch (Exception e) {
            logError("Errore durante la creazione dell'offerta", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verifica autenticazione
        Utente utente = requireAuthentication(request, response);
        if (utente == null) return;

        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo != null && pathInfo.matches("/asta/\\d+")) {
                // GET /api/offerte/asta/{id}
                handleGetOfferteByAsta(request, response, pathInfo);
            } else if (pathInfo != null && pathInfo.matches("/asta/\\d+/massima")) {
                // GET /api/offerte/asta/{id}/massima
                handleGetOffertaMassima(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Endpoint non trovato");
            }
        } catch (Exception e) {
            logError("Errore durante il recupero delle offerte", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }

    private void handleGetOfferteByAsta(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Estrai ID asta dal path: /asta/{id}
            String[] parts = pathInfo.split("/");
            int astaId = Integer.parseInt(parts[2]); // parts[0]="", parts[1]="asta", parts[2]=id
            
            List<Offerta> offerte = offertaDAO.getOfferteByAsta(astaId);
            logInfo("Recuperate " + offerte.size() + " offerte per asta: " + astaId);
            sendJSONResponse(response, offerte);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "ID asta non valido");
        } catch (ArrayIndexOutOfBoundsException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Formato URL non valido");
        }
    }

    private void handleGetOffertaMassima(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            // Estrai ID asta dal path: /asta/{id}/massima
            String[] parts = pathInfo.split("/");
            int astaId = Integer.parseInt(parts[2]);
            
            Double offertaMassima = offertaDAO.getOffertaMassima(astaId);
            
            Map<String, Object> result = Map.of(
                    "astaId", astaId,
                    "offertaMassima", offertaMassima != null ? offertaMassima : 0.0
            );
            
            logInfo("Offerta massima per asta " + astaId + ": €" + 
                    (offertaMassima != null ? offertaMassima : "nessuna"));
            sendJSONResponse(response, result);
            
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "ID asta non valido");
        } catch (ArrayIndexOutOfBoundsException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Formato URL non valido");
        }
    }
}