package it.polimi.servlet.api;

import it.polimi.dao.ArticoloDAO;
import it.polimi.model.Articolo;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet(name = "ArticoliAPIServlet", urlPatterns = {"/api/articoli", "/api/articoli/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1,    // 1 MB
        maxFileSize = 1024 * 1024 * 10,         // 10 MB
        maxRequestSize = 1024 * 1024 * 15       // 15 MB
)
public class ArticoliAPIServlet extends BaseAPIServlet {
    private ArticoloDAO articoloDAO = new ArticoloDAO();

    // Directory per salvare le immagini (relativa alla webapp)
    private static final String UPLOAD_DIR = "uploads" + File.separator + "images";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verifica autenticazione
        Utente utente = requireAuthentication(request, response);
        if (utente == null) return;

        try {
            // Controlla se è una richiesta multipart (con file)
            String contentType = request.getContentType();
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                handleMultipartRequest(request, response, utente);
            } else {
                handleJSONRequest(request, response, utente);
            }

        } catch (Exception e) {
            logError("Errore durante la creazione dell'articolo", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore interno del server");
        }
    }

    /**
     * Gestisce richieste multipart (con file)
     */
    private void handleMultipartRequest(HttpServletRequest request, HttpServletResponse response, Utente utente)
            throws IOException, ServletException {

        // Leggi parametri dal form
        String codice = request.getParameter("codice");
        String nome = request.getParameter("nome");
        String descrizione = request.getParameter("descrizione");
        String prezzoStr = request.getParameter("prezzo");

        // Validazione parametri base
        if (codice == null || nome == null || descrizione == null || prezzoStr == null ||
                codice.trim().isEmpty() || nome.trim().isEmpty() || descrizione.trim().isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Tutti i campi sono obbligatori");
            return;
        }

        double prezzo;
        try {
            prezzo = Double.parseDouble(prezzoStr);
            if (prezzo <= 0) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Il prezzo deve essere maggiore di zero");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Formato prezzo non valido");
            return;
        }

        // Gestione upload immagine
        Part filePart = request.getPart("immagine");
        String nomeFileImmagine = null;

        if (filePart != null && filePart.getSize() > 0) {
            nomeFileImmagine = salvaImmagine(filePart, request);
            if (nomeFileImmagine == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Errore nel caricamento dell'immagine. Formati supportati: JPG, PNG, GIF, WEBP (max 10MB)");
                return;
            }
        }

        // Crea oggetto Articolo
        Articolo articolo = new Articolo();
        articolo.setCodice(codice.trim());
        articolo.setNome(nome.trim());
        articolo.setDescrizione(descrizione.trim());
        articolo.setImmagine(nomeFileImmagine);
        articolo.setPrezzo(prezzo);
        articolo.setProprietarioId(utente.getId());
        articolo.setVenduto(false);

        logInfo("Creazione articolo con immagine: " + articolo.getCodice() + " per utente: " + utente.getUsername());

        // Salva nel database
        if (articoloDAO.creaArticolo(articolo)) {
            logInfo("Articolo creato con successo: " + articolo.getCodice());
            sendSuccessResponse(response, "Articolo creato con successo", articolo);
        } else {
            logError("Errore creazione articolo: " + articolo.getCodice(), null);

            // Se c'è stato un errore, elimina l'immagine caricata
            if (nomeFileImmagine != null) {
                eliminaImmagine(nomeFileImmagine, request);
            }

            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nella creazione dell'articolo");
        }
    }

    /**
     * Gestisce richieste JSON (backward compatibility)
     */
    private void handleJSONRequest(HttpServletRequest request, HttpServletResponse response, Utente utente)
            throws IOException {

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
        // Nessuna immagine per richieste JSON

        logInfo("Creazione articolo JSON: " + articolo.getCodice() + " per utente: " + utente.getUsername());

        // Salva nel database
        if (articoloDAO.creaArticolo(articolo)) {
            logInfo("Articolo creato con successo: " + articolo.getCodice());
            sendSuccessResponse(response, "Articolo creato con successo", articolo);
        } else {
            logError("Errore creazione articolo: " + articolo.getCodice(), null);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore nella creazione dell'articolo");
        }
    }

    /**
     * Salva l'immagine caricata sul server
     */
    private String salvaImmagine(Part filePart, HttpServletRequest request) throws IOException {
        String fileName = getFileName(filePart);

        if (fileName == null || fileName.isEmpty()) {
            logError("Nome file vuoto", null);
            return null;
        }

        // Verifica estensione
        String fileExtension = getFileExtension(fileName).toLowerCase();
        boolean validExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (fileExtension.equals(ext)) {
                validExtension = true;
                break;
            }
        }

        if (!validExtension) {
            logError("Estensione non valida: " + fileExtension, null);
            return null;
        }

        // Verifica dimensione
        if (filePart.getSize() > MAX_FILE_SIZE) {
            logError("File troppo grande: " + filePart.getSize() + " bytes", null);
            return null;
        }

        // Genera nome file unico
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Crea directory se non esiste
        String applicationPath = request.getServletContext().getRealPath("");
        String uploadPath = applicationPath + File.separator + UPLOAD_DIR;

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Salva il file
        String filePath = uploadPath + File.separator + uniqueFileName;

        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            logInfo("Immagine salvata: " + uniqueFileName);
            return uniqueFileName;
        } catch (IOException e) {
            logError("Errore salvataggio file: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Elimina un'immagine dal server
     */
    private void eliminaImmagine(String nomeFile, HttpServletRequest request) {
        if (nomeFile == null || nomeFile.isEmpty()) return;

        try {
            String applicationPath = request.getServletContext().getRealPath("");
            String filePath = applicationPath + File.separator + UPLOAD_DIR + File.separator + nomeFile;

            File file = new File(filePath);
            if (file.exists() && file.delete()) {
                logInfo("Immagine eliminata: " + nomeFile);
            } else {
                logError("Impossibile eliminare file: " + nomeFile, null);
            }
        } catch (Exception e) {
            logError("Errore eliminazione file: " + e.getMessage(), e);
        }
    }

    /**
     * Estrae il nome del file dal Part
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String token : contentDisposition.split(";")) {
                if (token.trim().startsWith("filename")) {
                    return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }

    /**
     * Estrae l'estensione del file
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) return "";
        return fileName.substring(lastDotIndex);
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