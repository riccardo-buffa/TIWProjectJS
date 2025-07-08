package it.polimi.servlet;

import it.polimi.dao.ArticoloDAO;
import it.polimi.dao.AstaDAO;
import it.polimi.model.Articolo;
import it.polimi.model.Asta;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "VendoServlet", urlPatterns = {"/vendo"})
public class VendoServlet extends HttpServlet {
    private ArticoloDAO articoloDAO = new ArticoloDAO();
    private AstaDAO astaDAO = new AstaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            response.sendRedirect("login.html");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        System.out.println("📦 [Jakarta] Caricamento pagina Vendo per: " + utente.getUsername());

        try {
            List<Asta> asteAperte = astaDAO.getAsteByVenditore(utente.getId(), false);
            List<Asta> asteChiuse = astaDAO.getAsteByVenditore(utente.getId(), true);
            List<Articolo> articoliDisponibili = articoloDAO.getArticoliDisponibili(utente.getId());

            System.out.println("📊 [Jakarta] Dati caricati - Aste aperte: " + asteAperte.size() +
                    ", Aste chiuse: " + asteChiuse.size() +
                    ", Articoli disponibili: " + articoliDisponibili.size());

            request.setAttribute("utente", utente);
            request.setAttribute("asteAperte", asteAperte);
            request.setAttribute("asteChiuse", asteChiuse);
            request.setAttribute("articoliDisponibili", articoliDisponibili);

            request.getRequestDispatcher("/WEB-INF/jsp/vendo.jsp").forward(request, response);

        } catch (Exception e) {
            System.err.println("❌ [Jakarta] Errore caricamento pagina Vendo: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore caricamento dati");
        }
    }
}