
package it.polimi.servlet;

import it.polimi.dao.UtenteDAO;
import it.polimi.model.Utente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {
    private UtenteDAO utenteDAO = new UtenteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("login.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        System.out.println("🔐 [Jakarta] Tentativo login per: " + username);

        Utente utente = utenteDAO.login(username, password);

        if (utente != null) {
            HttpSession session = request.getSession();
            session.setAttribute("utente", utente);
            System.out.println("✅ [Jakarta] Login successo per: " + username);
            response.sendRedirect("home");
        } else {
            System.out.println("❌ [Jakarta] Login fallito per: " + username);
            request.setAttribute("errore", "Username o password non corretti!");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/jsp/login-error.jsp").forward(request, response);
        }
    }
}
