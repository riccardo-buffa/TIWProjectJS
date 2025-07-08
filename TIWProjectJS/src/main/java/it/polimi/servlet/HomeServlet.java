
package it.polimi.servlet;

import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "HomeServlet", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utente") == null) {
            System.out.println("❌ [Jakarta] Accesso negato - Utente non loggato");
            response.sendRedirect("login.html");
            return;
        }

        Utente utente = (Utente) session.getAttribute("utente");
        System.out.println("✅ [Jakarta] Accesso home per: " + utente.getUsername());

        request.setAttribute("utente", utente);
        request.getRequestDispatcher("/WEB-INF/jsp/home.jsp").forward(request, response);
    }
}
