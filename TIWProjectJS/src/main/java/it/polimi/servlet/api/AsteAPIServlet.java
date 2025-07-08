package it.polimi.servlet.api;

import it.polimi.dao.ArticoloDAO;
import it.polimi.dao.AstaDAO;
import it.polimi.dao.OffertaDAO;
import it.polimi.model.Asta;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet(name = "AsteAPIServlet", urlPatterns = {"/api/aste", "/api/aste/*"})
public class AsteAPIServlet extends BaseAPIServlet {
    private AstaDAO astaDAO = new AstaDAO();
    private ArticoloDAO articoloDAO = new ArticoloDAO();
    private OffertaDAO offertaDAO = new OffertaDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        
        if (pathInfo != null && pathInfo.matches("/\\d+/chiudi")) {
            // POST /api/aste/{id}/chiudi
            handleChiudiAsta(request, response, pathInfo);
        } else if (pathInfo == null || pathInfo.equals("/")) {
            // POST /api/aste - crea nuova asta
            handleCreaAsta(request, response);
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "Endpoint non trovato");
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
                // GET /api/aste - possibile con parametro ids
                handleGetAste(request, response, utente);
            } else if (pathInfo.equals("/venditore")) {
                // GET /api/aste/venditore
                handleGetAsteByVenditore(request, response, utente);
            } else if (pathInfo.equals("/search")) {
                // GET /api/aste/search
                handleSearchAste(request, response);
            } else if (pathInfo.equals("/vinte")) {
                // GET /api/aste/vinte
                handleGetAsteVinte(request, response, utente);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/aste/{id}
                handleGetAstaById(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Endpoint non trovato");
            }
        } catch (Exception e) {
            logError("Errore durante il recupero delle aste", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }

    private void handleCreaAsta(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // Verifica autenticazione
        Utente utente = requireAuthentication(request, response);
        if (utente == null) return;

        try {
            // Leggi dati dal JSON
            String jsonString = readJSONFromRequest(request);
            Map<String, Object> astaData = parseJSONToMap(jsonString);

            // Valida parametri obbligatori
            if (!validateRequiredParams(response, astaData, "articoli", "rialzoMinimo", "scadenza")) {
                return;
            }

            // Estrai dati
            @SuppressWarnings("unchecked")
            List<Double> articoliDouble = (List<Double>) astaData.get("articoli");
            List<Integer> articoliIds = articoliDouble.stream()
                    .map(Double::intValue)
                    .collect(Collectors.toList());
            
            int rialzoMinimo = getIntFromDouble(astaData.get("rialzoMinimo"));
            String scadenzaStr = (String) astaData.get("scadenza");

            // Valida scadenza
            LocalDateTime scadenza;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                scadenza = LocalDateTime.parse(scadenzaStr, formatter);
                
                if (scadenza.isBefore(LocalDateTime.now())) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                            "La scadenza deve essere nel futuro");
                    return;
                }
            } catch (Exception e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Formato scadenza non valido (dd-MM-yyyy HH:mm)");
                return;
            }

            // Verifica che gli articoli non siano già in aste attive
            List<Integer> articoliGiaInAsta = articoloDAO.getArticoliGiaInAsteAttive(articoliIds);
            if (!articoliGiaInAsta.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_CONFLICT, 
                        "Alcuni articoli sono già in aste attive: " + articoliGiaInAsta);
                return;
            }

            // Calcola prezzo iniziale
            List<it.polimi.model.Articolo> articoli = articoloDAO.getArticoliByIds(articoliIds);
            double prezzoIniziale = articoli.stream()
                    .mapToDouble(it.polimi.model.Articolo::getPrezzo)
                    .sum();

            // Crea l'asta
            Asta asta = new Asta(prezzoIniziale, rialzoMinimo, scadenza, utente.getId());

            logInfo("Creazione asta con " + articoliIds.size() + " articoli per utente: " + utente.getUsername());

            if (astaDAO.creaAsta(asta, articoliIds)) {
                logInfo("Asta creata con successo");
                sendSuccessResponse(response, "Asta creata con successo", asta);
            } else {
                logError("Errore creazione asta", null);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Errore nella creazione dell'asta");
            }

        } catch (Exception e) {
            logError("Errore durante la creazione dell'asta", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }

    private void handleGetAste(HttpServletRequest request, HttpServletResponse response, Utente utente) 
            throws IOException {
        
        String idsParam = request.getParameter("ids");
        
        if (idsParam != null && !idsParam.trim().isEmpty()) {
            try {
                List<Integer> ids = Arrays.stream(idsParam.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                
                // Per semplicità, restituiamo una lista vuota
                // In una implementazione completa, avresti un metodo getAsteByIds
                logInfo("Richiesta aste per IDs: " + ids);
                sendJSONResponse(response, List.of());
                
            } catch (NumberFormatException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Formato IDs non valido");
            }
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Parametro 'ids' richiesto");
        }
    }

    private void handleGetAsteByVenditore(HttpServletRequest request, HttpServletResponse response, Utente utente) 
            throws IOException {
        
        String chiuseParam = request.getParameter("chiuse");
        boolean chiuse = "true".equalsIgnoreCase(chiuseParam);
        
        List<Asta> aste = astaDAO.getAsteByVenditore(utente.getId(), chiuse);
        logInfo("Recuperate " + aste.size() + " aste " + (chiuse ? "chiuse" : "aperte") + 
                " per venditore: " + utente.getUsername());
        sendJSONResponse(response, aste);
    }

    private void handleSearchAste(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String query = request.getParameter("q");
        if (query == null || query.trim().isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Parametro 'q' richiesto per la ricerca");
            return;
        }
        
        List<Asta> aste = astaDAO.cercaAste(query.trim());
        logInfo("Ricerca aste per: '" + query + "' - Trovate: " + aste.size());
        sendJSONResponse(response, aste);
    }

    private void handleGetAsteVinte(HttpServletRequest request, HttpServletResponse response, Utente utente) 
            throws IOException {
        
        List<Asta> aste = astaDAO.getAsteVinte(utente.getId());
        logInfo("Recuperate " + aste.size() + " aste vinte per utente: " + utente.getUsername());
        sendJSONResponse(response, aste);
    }

    private void handleGetAstaById(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        try {
            int astaId = Integer.parseInt(pathInfo.substring(1)); // Rimuovi '/'
            Asta asta = astaDAO.getById(astaId);
            
            if (asta != null) {
                logInfo("Recuperata asta: " + astaId);
                sendJSONResponse(response, asta);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Asta non trovata");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "ID asta non valido");
        }
    }

    private void handleChiudiAsta(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        // Verifica autenticazione
        Utente utente = requireAuthentication(request, response);
        if (utente == null) return;

        try {
            // Estrai ID asta dal path
            String idStr = pathInfo.substring(1, pathInfo.lastIndexOf("/"));
            int astaId = Integer.parseInt(idStr);
            
            // Verifica esistenza e autorizzazione
            Asta asta = astaDAO.getById(astaId);
            if (asta == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Asta non trovata");
                return;
            }
            
            if (asta.getVenditoreId() != utente.getId()) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, 
                        "Non autorizzato a chiudere questa asta");
                return;
            }
            
            if (asta.isChiusa()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "L'asta è già stata chiusa");
                return;
            }
            
            if (!asta.isScaduta()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "L'asta non è ancora scaduta");
                return;
            }

            // Determina vincitore e prezzo finale
            Double offertaMassima = offertaDAO.getOffertaMassima(astaId);
            Integer vincitoreId = null;
            Double prezzoFinale = null;

            if (offertaMassima != null && offertaMassima > 0) {
                vincitoreId = offertaDAO.getVincitore(astaId);
                prezzoFinale = offertaMassima;
                
                // Marca articoli come venduti se c'è un vincitore
                if (vincitoreId != null && vincitoreId > 0) {
                    List<Integer> articoliIds = asta.getArticoli().stream()
                            .map(it.polimi.model.Articolo::getId)
                            .collect(Collectors.toList());
                    articoloDAO.marcaVenduti(articoliIds);
                }
            }

            // Chiudi l'asta
            boolean success = astaDAO.chiudiAsta(astaId, vincitoreId, prezzoFinale);
            
            if (success) {
                logInfo("Asta " + astaId + " chiusa con successo");
                sendSuccessResponse(response, "Asta chiusa con successo");
            } else {
                logError("Errore chiusura asta " + astaId, null);
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Errore durante la chiusura dell'asta");
            }

        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "ID asta non valido");
        } catch (Exception e) {
            logError("Errore durante la chiusura dell'asta", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Errore interno del server");
        }
    }
}