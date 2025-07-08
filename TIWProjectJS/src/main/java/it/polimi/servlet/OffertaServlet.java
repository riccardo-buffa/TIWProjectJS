
package it.polimi.servlet;

import it.polimi.dao.AstaDAO;
import it.polimi.dao.OffertaDAO;
import it.polimi.model.Asta;
import it.polimi.model.Offerta;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "OffertaServlet", urlPatterns = {"/offerta"})
public class OffertaServlet extends HttpServlet {
    private AstaDAO astaDAO = new AstaDAO();
    private OffertaDAO offertaDAO = new OffertaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        mostraFormOfferta(request, response, null);
    }

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
            int astaId = Integer.parseInt(request.getParameter("astaId"));
            double importoOfferta = Double.parseDouble(request.getParameter("importo"));

            System.out.println("💰 [Jakarta] Offerta di €" + importoOfferta + " per asta " + astaId + " da utente: " + utente.getUsername());

            Asta asta = astaDAO.getById(astaId);
            if (asta == null || asta.isChiusa() || asta.isScaduta()) {
                System.err.println("❌ [Jakarta] Asta non valida o chiusa: " + astaId);
                mostraFormOfferta(request, response, "Asta non valida o chiusa");
                return;
            }

            double offertaMassima = asta.getOffertaMassima();
            double minimaRichiesta = offertaMassima + asta.getRialzoMinimo();

            if (importoOfferta < minimaRichiesta) {
                System.err.println("❌ [Jakarta] Offerta troppo bassa: €" + importoOfferta + " < €" + minimaRichiesta);
                mostraFormOfferta(request, response, "L'offerta deve essere almeno €" + minimaRichiesta);
                return;
            }

            Offerta offerta = new Offerta(astaId, utente.getId(), importoOfferta);

            if (offertaDAO.creaOfferta(offerta)) {
                System.out.println("✅ [Jakarta] Offerta registrata con successo");
                mostraFormOfferta(request, response, "Offerta inviata con successo!");
            } else {
                System.err.println("❌ [Jakarta] Errore registrazione offerta");
                mostraFormOfferta(request, response, "Errore nell'invio dell'offerta");
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ [Jakarta] Errore formato numerico: " + e.getMessage());
            mostraFormOfferta(request, response, "Formato numerico non valido");
        } catch (Exception e) {
            System.err.println("❌ [Jakarta] Errore gestione offerta: " + e.getMessage());
            e.printStackTrace();
            mostraFormOfferta(request, response, "Errore nell'elaborazione dell'offerta");
        }
    }

    private void mostraFormOfferta(HttpServletRequest request, HttpServletResponse response, String messaggio)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            response.sendRedirect("login.html");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");

        try {
            int astaId = Integer.parseInt(request.getParameter("id") != null ?
                    request.getParameter("id") : request.getParameter("astaId"));

            Asta asta = astaDAO.getById(astaId);
            if (asta == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Asta non trovata");
                return;
            }

            List<Offerta> offerte = offertaDAO.getOfferteByAsta(astaId);

            // Calcola offerta minima richiesta
            double offertaMassima = asta.getOffertaMassima();
            double minimaRichiesta = offertaMassima + asta.getRialzoMinimo();

            request.setAttribute("utente", utente);
            request.setAttribute("asta", asta);
            request.setAttribute("offerte", offerte);
            request.setAttribute("minimaRichiesta", minimaRichiesta);
            request.setAttribute("messaggio", messaggio);

            request.getRequestDispatcher("/WEB-INF/jsp/offerta.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ [Jakarta] Errore caricamento form offerta: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore caricamento form");
        }
    }
}
