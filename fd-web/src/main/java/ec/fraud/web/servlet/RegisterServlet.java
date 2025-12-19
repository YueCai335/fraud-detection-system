package ec.fraud.web.servlet;

import ec.fraud.web.db.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;

public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String firstName = trim(request.getParameter("first_name"));
            String lastName = trim(request.getParameter("last_name"));
            String email = trim(request.getParameter("email"));
            String username = trim(request.getParameter("username"));
            String password = request.getParameter("password");
            String password2 = request.getParameter("password2");

            if (isEmpty(firstName) || isEmpty(lastName) || isEmpty(email) || isEmpty(username)
                    || isEmpty(password) || isEmpty(password2)) {
                request.setAttribute("error", "All fields are required.");
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }

            if (!password.equals(password2)) {
                request.setAttribute("error", "Password and confirm password do not match.");
                request.getRequestDispatcher("/register.jsp").forward(request, response);
                return;
            }

            String passwordHash = sha256Hex(password);
            LocalDate today = LocalDate.now();

            insertUser(firstName, lastName, email, username, passwordHash, today);

            request.setAttribute("msg", "Registration successful. You can now login (if login is implemented) or proceed.");
        } catch (SQLIntegrityConstraintViolationException dup) {
            request.setAttribute("error", "Username already exists. Please choose a different username.");
        } catch (Exception ex) {
            request.setAttribute("error", ex.toString());
        }

        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    private void insertUser(String firstName, String lastName, String email,
                            String username, String passwordHash, LocalDate regdate)
            throws SQLException {

        String sql = "INSERT INTO `user` (first_name, last_name, email, username, password, regdate) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, username);
            ps.setString(5, passwordHash);
            ps.setDate(6, java.sql.Date.valueOf(regdate));

            ps.executeUpdate();
        }
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    private String trim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
