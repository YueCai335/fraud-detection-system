package ec.fraud.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Guide page controller.
 * Only checks session and forwards to guide.jsp.
 */
public class GuideServlet extends HttpServlet {

    // =========================
    // Session check helpers
    // =========================
    private boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("username") != null;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isLoggedIn(request)) {
            redirectToLogin(request, response);
            return;
        }

        // for sidebar highlight
        request.setAttribute("activePage", "guide");

        request.getRequestDispatcher("/guide.jsp").forward(request, response);
    }
}
