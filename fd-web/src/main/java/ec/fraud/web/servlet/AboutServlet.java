package ec.fraud.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/about")
public class AboutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 如果你后面想显示版本号/运行状态，也可以在这里 setAttribute
        // request.setAttribute("version", "1.0");

        request.getRequestDispatcher("/about.jsp").forward(request, response);
    }
}