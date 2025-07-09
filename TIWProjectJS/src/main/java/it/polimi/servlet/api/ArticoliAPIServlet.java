package it.polimi.servlet.api;

import it.polimi.dao.ArticoloDAO;
import it.polimi.model.Articolo;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@WebServlet(name = "ArticoliAPIServlet", urlPatterns = {"/api/articoli", "/api/articoli/*"})
public class ArticoliAPIServlet extends BaseAPIServlet {
    private ArticoloDAO articoloDAO = new ArticoloDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verifica autenticazione
        Utente utente = requireAuthentication(request, response);
        if (utente == null) return;

        try {
            // Leggi dati dal JSON
            String jsonString = readJSONFromRequest(request);
            Map<String, Object> articoloData = parseJSONToMap(jsonString);

            // Valida parametri obbligatori
            if (!validateRequiredParams(response, articoloData, "codice", "nome", "descrizione", "prezzo")) {
                return;
            }

            // Crea oggetto Articolo
            Articolo articolo = new Articolo();
            articolo.setCodice((String) articoloData.get("codice"));
            articolo.setNome((String) articoloData.get("nome"));
            articolo.setDescrizione((String) articoloData.get("descrizione"));
            articolo.setPrezzo(((Number) articoloData.get("prezzo")).doubleValue());
            articolo.setProprietarioId(utente.getId());
            articolo.setVenduto(false);

            logInfo("Creazione articolo: " + articolo.getCodice() + " per utente: " + utente.getUsername());

            // Salva nel database
            if (articoloDAO.creaArticolo(articolo)) {
                logInfo("Articolo creato con successo: " + articolo.getCodice());
                sendSuccessResponse(response, "Articolo creato con successo", articolo);
            } else {
                logError("Errore creazione articolo: " + articolo.getCodice(), null);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Errore nella creazione dell'articolo");
            }

        } catch (Exception e) {
            logError("Errore durante la creazione dell'articolo", e);
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
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/articoli - possibile con parametro ids
                handleGetArticoli(request, response, utente);
            } else if (pathInfo.equals("/disponibili")) {
                // GET /api/articoli/disponibili
                handleGetArticoliDisponibili(request, response, utente);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Endpoint non trovato");
            }
        } catch (Exception e) {
            logError("Errore durante il recupero degli articoli", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }

    private void handleGetArticoli(HttpServletRequest request, HttpServletResponse response, Utente utente) 
            throws IOException {
        
        String idsParam = request.getParameter("ids");
        
        if (idsParam != null && !idsParam.trim().isEmpty()) {
            // Recupera articoli per IDs specifici
            try {
                List<Integer> ids = Arrays.stream(idsParam.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                
                List<Articolo> articoli = articoloDAO.getArticoliByIds(ids);
                logInfo("Recuperati " + articoli.size() + " articoli per IDs: " + ids);
                sendJSONResponse(response, articoli);
                
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Formato IDs non valido");
            }
        } else {
            // Senza parametri, ritorna errore
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Parametro 'ids' richiesto");
        }
    }

    private void handleGetArticoliDisponibili(HttpServletRequest request, HttpServletResponse response, Utente utente) 
            throws IOException {
        
        List<Articolo> articoli = articoloDAO.getArticoliDisponibili(utente.getId());
        logInfo("Recuperati " + articoli.size() + " articoli disponibili per utente: " + utente.getUsername());
        sendJSONResponse(response, articoli);
    }
}