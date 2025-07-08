package it.polimi.servlet;

import it.polimi.dao.ArticoloDAO;
import it.polimi.model.Articolo;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@WebServlet(name = "CreaArticoloServlet", urlPatterns = {"/crea-articolo"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1,    // 1 MB
        maxFileSize = 1024 * 1024 * 10,         // 10 MB
        maxRequestSize = 1024 * 1024 * 15       // 15 MB
)
public class CreaArticoloServlet extends HttpServlet {
    private ArticoloDAO articoloDAO = new ArticoloDAO();

    // Directory per salvare le immagini (relativa alla webapp)
    private static final String UPLOAD_DIR = "uploads" + File.separator + "images";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            response.sendRedirect("login.html");
            return;
        }

        request.setCharacterEncoding("UTF-8");
        Utente utente = (Utente) session.getAttribute("utente");

        try {
            String codice = request.getParameter("codice");
            String nome = request.getParameter("nome");
            String descrizione = request.getParameter("descrizione");
            double prezzo = Double.parseDouble(request.getParameter("prezzo"));

            // Gestione upload immagine
            Part filePart = request.getPart("immagine");
            String nomeFileImmagine = null;

            if (filePart != null && filePart.getSize() > 0) {
                nomeFileImmagine = salvaImmagine(filePart, request);
                if (nomeFileImmagine == null) {
                    // Errore nel salvare l'immagine
                    request.setAttribute("errore", "❌ Errore nel caricamento dell'immagine. Formati supportati: JPG, PNG, GIF (max 10MB)");
                    request.setAttribute("codice", codice);
                    request.setAttribute("nome", nome);
                    request.setAttribute("descrizione", descrizione);
                    request.setAttribute("prezzo", prezzo);
                    request.getRequestDispatcher("/WEB-INF/jsp/crea-articolo-error.jsp").forward(request, response);
                    return;
                }
            }

            System.out.println("📦 [Jakarta] Creazione articolo: " + codice + " - " + nome +
                    (nomeFileImmagine != null ? " con immagine: " + nomeFileImmagine : " senza immagine"));

            Articolo articolo = new Articolo(codice, nome, descrizione, nomeFileImmagine, prezzo, utente.getId());

            if (articoloDAO.creaArticolo(articolo)) {
                System.out.println("✅ [Jakarta] Articolo creato con successo: " + codice);
                response.sendRedirect("vendo");
            } else {
                System.err.println("❌ [Jakarta] Errore creazione articolo nel database: " + codice);

                // Se c'è stato un errore, elimina l'immagine caricata
                if (nomeFileImmagine != null) {
                    eliminaImmagine(nomeFileImmagine, request);
                }

                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nella creazione dell'articolo");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ [Jakarta] Prezzo non valido: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Prezzo non valido");
        } catch (Exception e) {
            System.err.println("❌ [Jakarta] Errore creazione articolo: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nella creazione dell'articolo");
        }
    }

    /**
     * Salva l'immagine caricata sul server
     */
    private String salvaImmagine(Part filePart, HttpServletRequest request) throws IOException {
        String fileName = getFileName(filePart);

        if (fileName == null || fileName.isEmpty()) {
            System.err.println("❌ [Upload] Nome file vuoto");
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
            System.err.println("❌ [Upload] Estensione non valida: " + fileExtension);
            return null;
        }

        // Verifica dimensione
        if (filePart.getSize() > MAX_FILE_SIZE) {
            System.err.println("❌ [Upload] File troppo grande: " + filePart.getSize() + " bytes");
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
            System.out.println("📁 [Upload] Creata directory: " + uploadPath);
        }

        // Salva il file
        String filePath = uploadPath + File.separator + uniqueFileName;

        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ [Upload] Immagine salvata: " + uniqueFileName);
            return uniqueFileName;
        } catch (IOException e) {
            System.err.println("❌ [Upload] Errore salvataggio file: " + e.getMessage());
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
                System.out.println("🗑️ [Upload] Immagine eliminata: " + nomeFile);
            }
        } catch (Exception e) {
            System.err.println("❌ [Upload] Errore eliminazione file: " + e.getMessage());
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
}