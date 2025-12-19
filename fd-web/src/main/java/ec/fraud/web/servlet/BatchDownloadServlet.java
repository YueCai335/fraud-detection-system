package ec.fraud.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BatchDownloadServlet extends HttpServlet {

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

        HttpSession session = request.getSession(false);
        String token = request.getParameter("token");

        if (session == null || token == null || token.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing token.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> tokenMap = (Map<String, String>) session.getAttribute("BATCH_TOKEN_MAP");

        if (tokenMap == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No batch results found in session.");
            return;
        }

        String path = tokenMap.get(token);
        if (path == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Invalid token.");
            return;
        }

        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File not found.");
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"fd_batch_predictions.csv\"");

        try (InputStream in = new FileInputStream(f);
             OutputStream out = response.getOutputStream()) {

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
    }
}
