package ec.fraud.web.servlet;

import ec.fraud.soapclient.PredictResponse2;
import ec.fraud.web.soap.FraudSoapClient;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

public class PredictServlet extends HttpServlet {

    private FraudSoapClient client;

    @Override
    public void init() throws ServletException {
        client = new FraudSoapClient();
    }

    private boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("username") != null;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isLoggedIn(request)) {
            redirectToLogin(request, response);
            return;
        }

        request.getRequestDispatcher("/predict.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isLoggedIn(request)) {
            redirectToLogin(request, response);
            return;
        }

        try {
            int step = Integer.parseInt(request.getParameter("step"));
            int typeCode = Integer.parseInt(request.getParameter("type_code"));

            double amount = Double.parseDouble(request.getParameter("amount"));
            double oldbalanceOrg = Double.parseDouble(request.getParameter("oldbalanceOrg"));
            double newbalanceOrig = Double.parseDouble(request.getParameter("newbalanceOrig"));
            double oldbalanceDest = Double.parseDouble(request.getParameter("oldbalanceDest"));
            double newbalanceDest = Double.parseDouble(request.getParameter("newbalanceDest"));

            PredictResponse2 result = client.predict(
                    step, typeCode, amount,
                    oldbalanceOrg, newbalanceOrig,
                    oldbalanceDest, newbalanceDest
            );

            request.setAttribute("result", result);

        } catch (Exception ex) {
            request.setAttribute("error", ex.toString());
        }

        // forward back to the same page (single-page UX)
        request.getRequestDispatcher("/predict.jsp").forward(request, response);
    }
}
