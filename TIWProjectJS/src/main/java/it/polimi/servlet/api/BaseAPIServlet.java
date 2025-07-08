package it.polimi.servlet.api;

import it.polimi.model.Utente;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Classe base per tutti i servlet API
 * Fornisce funzionalità comuni come parsing JSON, gestione sessioni, etc.
 */
public abstract class BaseAPIServlet extends HttpServlet {
    
    protected final Gson gson;
    
    public BaseAPIServlet() {
        // Configura Gson per gestire LocalDateTime
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) 
                        (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) 
                        (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
                .create();
    }

    /**
     * Configura la risposta per JSON con CORS
     */
    protected void setupJSONResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Legge il JSON dal body della richiesta
     */
    protected String readJSONFromRequest(HttpServletRequest request) throws IOException {
        StringBuilder jsonBuffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuffer.append(line);
        }
        return jsonBuffer.toString();
    }

    /**
     * Converte JSON string in oggetto Map
     */
    protected Map<String, Object> parseJSONToMap(String json) {
        return gson.fromJson(json, Map.class);
    }

    /**
     * Converte JSON string in oggetto della classe specificata
     */
    protected <T> T parseJSON(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * Invia una risposta JSON
     */
    protected void sendJSONResponse(HttpServletResponse response, Object data) throws IOException {
        setupJSONResponse(response);
        response.getWriter().write(gson.toJson(data));
    }

    /**
     * Invia una risposta di errore JSON
     */
    protected void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        setupJSONResponse(response);
        response.setStatus(statusCode);
        
        Map<String, Object> errorResponse = Map.of(
                "error", true,
                "message", message,
                "status", statusCode
        );
        
        response.getWriter().write(gson.toJson(errorResponse));
    }

    /**
     * Invia una risposta di successo JSON
     */
    protected void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        sendSuccessResponse(response, message, null);
    }

    /**
     * Invia una risposta di successo JSON con dati
     */
    protected void sendSuccessResponse(HttpServletResponse response, String message, Object data) throws IOException {
        setupJSONResponse(response);
        
        Map<String, Object> successResponse = Map.of(
                "success", true,
                "message", message,
                "data", data != null ? data : new Object()
        );
        
        response.getWriter().write(gson.toJson(successResponse));
    }

    /**
     * Ottieni l'utente corrente dalla sessione
     */
    protected Utente getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Utente) session.getAttribute("utente");
    }

    /**
     * Verifica che l'utente sia autenticato
     */
    protected boolean isUserAuthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utente utente = getCurrentUser(request);
        if (utente == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Accesso negato. Effettuare il login.");
            return false;
        }
        return true;
    }

    /**
     * Verifica che l'utente sia autenticato e ritorna l'utente
     */
    protected Utente requireAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utente utente = getCurrentUser(request);
        if (utente == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
                    "Accesso negato. Effettuare il login.");
        }
        return utente;
    }

    /**
     * Gestisce le richieste OPTIONS per CORS
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setupJSONResponse(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Valida parametri obbligatori
     */
    protected boolean validateRequiredParams(HttpServletResponse response, Map<String, Object> params, String... requiredFields) throws IOException {
        for (String field : requiredFields) {
            if (!params.containsKey(field) || params.get(field) == null || 
                (params.get(field) instanceof String && ((String) params.get(field)).trim().isEmpty())) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                        "Il campo '" + field + "' è obbligatorio");
                return false;
            }
        }
        return true;
    }

    /**
     * Converte Double a int per gli ID
     */
    protected int getIntFromDouble(Object value) {
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new IllegalArgumentException("Valore non convertibile a int: " + value);
    }

    /**
     * Log per debug
     */
    protected void logDebug(String message) {
        System.out.println("🔧 [API Debug] " + message);
    }

    /**
     * Log per info
     */
    protected void logInfo(String message) {
        System.out.println("ℹ️ [API Info] " + message);
    }

    /**
     * Log per errori
     */
    protected void logError(String message, Exception e) {
        System.err.println("❌ [API Error] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}