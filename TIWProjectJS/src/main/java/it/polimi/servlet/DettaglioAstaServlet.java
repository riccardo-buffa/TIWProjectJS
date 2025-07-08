package it.polimi.servlet;

import it.polimi.dao.AstaDAO;
import it.polimi.dao.OffertaDAO;
import it.polimi.dao.UtenteDAO;
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

@WebServlet(name = "DettaglioAstaServlet", urlPatterns = {"/dettaglio-asta"})
public class DettaglioAstaServlet extends HttpServlet {
    private AstaDAO astaDAO = new AstaDAO();
    private OffertaDAO offertaDAO = new OffertaDAO();
    private UtenteDAO utenteDAO = new UtenteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            response.sendRedirect("login.html");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");

        try {
            int astaId = Integer.parseInt(request.getParameter("id"));
            System.out.println("🔍 [Jakarta] Dettaglio asta " + astaId + " per utente: " + utente.getUsername());

            Asta asta = astaDAO.getById(astaId);
            if (asta == null) {
                System.err.println("❌ [Jakarta] Asta non trovata: " + astaId);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Asta non trovata");
                return;
            }

            // Carica le offerte per l'asta
            List<Offerta> offerte = offertaDAO.getOfferteByAsta(astaId);

            // Carica informazioni del venditore
            Utente venditore = utenteDAO.getById(asta.getVenditoreId());

            // Carica informazioni del vincitore se presente
            Utente vincitore = null;
            if (asta.getVincitoreId() != null) {
                vincitore = utenteDAO.getById(asta.getVincitoreId());
                System.out.println("🏆 [Jakarta] Vincitore trovato: " + vincitore.getNomeCompleto() +
                        " con prezzo finale: €" + asta.getPrezzoFinale());
            }

            // Calcola statistiche dell'asta
            int numeroPartecipanti = 0;
            if (offerte != null && !offerte.isEmpty()) {
                numeroPartecipanti = (int) offerte.stream()
                        .map(Offerta::getOfferenteId)
                        .distinct()
                        .count();
            }

            // Determina il tipo di accesso (venditore, partecipante, vincitore, osservatore)
            boolean isVenditore = (asta.getVenditoreId() == utente.getId());
            boolean isVincitore = (asta.getVincitoreId() != null && asta.getVincitoreId() == utente.getId());
            boolean isPartecipante = offerte.stream()
                    .anyMatch(o -> o.getOfferenteId() == utente.getId());

            System.out.println("📊 [Jakarta] Asta caricata - ID: " + astaId +
                    ", Offerte: " + offerte.size() +
                    ", Partecipanti: " + numeroPartecipanti +
                    ", Venditore: " + isVenditore +
                    ", Vincitore: " + isVincitore +
                    ", Partecipante: " + isPartecipante +
                    (vincitore != null ? ", Aggiudicatario: " + vincitore.getNomeCompleto() : ""));

            // Imposta attributi per la JSP
            request.setAttribute("utente", utente);
            request.setAttribute("asta", asta);
            request.setAttribute("offerte", offerte);
            request.setAttribute("venditore", venditore);
            request.setAttribute("vincitore", vincitore);
            request.setAttribute("numeroPartecipanti", numeroPartecipanti);
            request.setAttribute("isVenditore", isVenditore);
            request.setAttribute("isVincitore", isVincitore);
            request.setAttribute("isPartecipante", isPartecipante);

            request.getRequestDispatcher("/WEB-INF/jsp/dettaglio-asta.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            System.err.println("❌ [Jakarta] ID asta non valido: " + request.getParameter("id"));
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID asta non valido");
        } catch (Exception e) {
            System.err.println("❌ [Jakarta] Errore caricamento dettaglio asta: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore caricamento dettagli");
        }
    }
}