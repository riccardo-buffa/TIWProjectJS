package it.polimi.servlet;

import it.polimi.dao.ArticoloDAO;
import it.polimi.dao.AstaDAO;
import it.polimi.model.Articolo;
import it.polimi.model.Asta;
import it.polimi.model.Utente;
import it.polimi.util.DateUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreaAstaServlet", urlPatterns = {"/crea-asta"})
public class CreaAstaServlet extends HttpServlet {
    private AstaDAO astaDAO = new AstaDAO();
    private ArticoloDAO articoloDAO = new ArticoloDAO();

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
            String[] articoliIdsStr = request.getParameterValues("articoli");
            if (articoliIdsStr == null || articoliIdsStr.length == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Devi selezionare almeno un articolo");
                return;
            }

            List<Integer> articoliIds = new ArrayList<>();
            for (String idStr : articoliIdsStr) {
                articoliIds.add(Integer.parseInt(idStr));
            }

            System.out.println("🎯 [Jakarta] Creazione asta con " + articoliIds.size() + " articoli per utente: " + utente.getUsername());

            // CONTROLLO: Verifica che gli articoli non siano già in altre aste attive
            List<Integer> articoliGiaInAsta = articoloDAO.getArticoliGiaInAsteAttive(articoliIds);
            if (!articoliGiaInAsta.isEmpty()) {
                System.err.println("❌ [Jakarta] Articoli già in aste attive: " + articoliGiaInAsta);

                // Trova i nomi degli articoli per il messaggio di errore
                List<Articolo> articoliProblematici = articoloDAO.getArticoliByIds(articoliGiaInAsta);
                StringBuilder nomiArticoli = new StringBuilder();
                for (Articolo art : articoliProblematici) {
                    if (nomiArticoli.length() > 0) nomiArticoli.append(", ");
                    nomiArticoli.append(art.getCodice()).append(" - ").append(art.getNome());
                }

                String messaggioErrore = "❌ I seguenti articoli sono già inseriti in altre aste attive: " + nomiArticoli.toString() +
                        ". Un articolo può essere inserito in una sola asta alla volta.";

                request.setAttribute("errore", messaggioErrore);
                request.setAttribute("articoliSelezionati", articoliIds);
                request.getRequestDispatcher("/WEB-INF/jsp/crea-asta-error.jsp").forward(request, response);
                return;
            }

            // Verifica che tutti gli articoli appartengano all'utente
            List<Articolo> articoli = articoloDAO.getArticoliByIds(articoliIds);
            for (Articolo articolo : articoli) {
                if (articolo.getProprietarioId() != utente.getId()) {
                    System.err.println("❌ [Jakarta] Tentativo di inserire articolo non proprio: " + articolo.getCodice());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Non puoi inserire articoli che non ti appartengono");
                    return;
                }
                if (articolo.isVenduto()) {
                    System.err.println("❌ [Jakarta] Tentativo di inserire articolo già venduto: " + articolo.getCodice());
                    request.setAttribute("errore", "❌ L'articolo " + articolo.getCodice() + " è già stato venduto e non può essere inserito in un'asta.");
                    request.getRequestDispatcher("/WEB-INF/jsp/crea-asta-error.jsp").forward(request, response);
                    return;
                }
            }

            // Calcola prezzo iniziale
            double prezzoIniziale = 0.0;
            for (Articolo articolo : articoli) {
                prezzoIniziale += articolo.getPrezzo();
                System.out.println("  - Articolo: " + articolo.getCodice() + " €" + articolo.getPrezzo());
            }

            int rialzoMinimo = Integer.parseInt(request.getParameter("rialzo"));
            LocalDateTime scadenza = DateUtil.parseDateTime(request.getParameter("scadenza"));

            if (scadenza == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato data non valido. Usa: dd-MM-yyyy HH:mm");
                return;
            }

            System.out.println("📊 [Jakarta] Prezzo iniziale: €" + prezzoIniziale + ", Rialzo: €" + rialzoMinimo + ", Scadenza: " + scadenza);

            Asta asta = new Asta(prezzoIniziale, rialzoMinimo, scadenza, utente.getId());

            if (astaDAO.creaAsta(asta, articoliIds)) {
                System.out.println("✅ [Jakarta] Asta creata con successo");
                response.sendRedirect("vendo");
            } else {
                System.err.println("❌ [Jakarta] Errore creazione asta");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nella creazione dell'asta");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ [Jakarta] Errore formato numerico: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato numerico non valido");
        } catch (Exception e) {
            System.err.println("❌ [Jakarta] Errore creazione asta: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nella creazione dell'asta");
        }
    }
}