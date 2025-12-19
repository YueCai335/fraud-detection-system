package ec.fraud.web.servlet;

import ec.fraud.soapclient.PredictResponse2;
import ec.fraud.web.soap.FraudSoapClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Batch CSV upload -> predict -> show Top 10 preview on same page (batch.jsp)
 * and provide a download token for full result CSV.
 */
@MultipartConfig
public class BatchServlet extends HttpServlet {

    private FraudSoapClient client;

    @Override
    public void init() throws ServletException {
        client = new FraudSoapClient();
    }

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

        request.getRequestDispatcher("/batch.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isLoggedIn(request)) {
            redirectToLogin(request, response);
            return;
        }

        HttpSession session = request.getSession();

        try {
            Part filePart = request.getPart("csvFile");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("error", "Missing csvFile upload. Please select a CSV file.");
                request.getRequestDispatcher("/batch.jsp").forward(request, response);
                return;
            }

            // Prepare preview rows (top 10) for JSP
            List<Map<String, String>> previewRows = new ArrayList<>();

            // Create a temp output CSV file for full download
            String token = UUID.randomUUID().toString();
            File outFile = File.createTempFile("fd-batch-" + token + "-", ".csv");

            // Keep token -> file path in session
            Map<String, String> tokenMap = getOrCreateTokenMap(session);
            tokenMap.put(token, outFile.getAbsolutePath());

            int totalPredicted = 0;

            try (
                    InputStream in = filePart.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))
            ) {
                // Output header for result CSV
                pw.println("step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest,fraud,prob_fraud");

                String line;
                boolean firstLine = true;

                // Track original CSV line number (1-based, includes header if present)
                int csvLineNumber = 0;

                while ((line = br.readLine()) != null) {
                    csvLineNumber++;

                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (firstLine) {
                        firstLine = false;
                        if (looksLikeHeader(line)) {
                            continue;
                        }
                    }

                    String[] cols = splitCsvLineSimple(line);
                    if (cols.length < 7) continue;

                    int step = parseIntSafe(cols[0]);
                    int typeCode = parseIntSafe(cols[1]);
                    double amount = parseDoubleSafe(cols[2]);
                    double oldbalanceOrg = parseDoubleSafe(cols[3]);
                    double newbalanceOrig = parseDoubleSafe(cols[4]);
                    double oldbalanceDest = parseDoubleSafe(cols[5]);
                    double newbalanceDest = parseDoubleSafe(cols[6]);

                    PredictResponse2 result = client.predict(
                            step, typeCode, amount,
                            oldbalanceOrg, newbalanceOrig,
                            oldbalanceDest, newbalanceDest
                    );

                    int fraud = result.getFraud();
                    double prob = result.getProbFraud();

                    pw.println(step + "," + typeCode + "," + amount + "," +
                            oldbalanceOrg + "," + newbalanceOrig + "," +
                            oldbalanceDest + "," + newbalanceDest + "," +
                            fraud + "," + prob);

                    totalPredicted++;

                    if (previewRows.size() < 10) {
                        Map<String, String> row = new HashMap<>();
                        row.put("row_num", String.valueOf(csvLineNumber)); // original CSV row number
                        row.put("step", String.valueOf(step));
                        row.put("type_code", String.valueOf(typeCode));
                        row.put("amount", String.valueOf(amount));
                        row.put("oldbalanceOrg", String.valueOf(oldbalanceOrg));
                        row.put("newbalanceOrig", String.valueOf(newbalanceOrig));
                        row.put("oldbalanceDest", String.valueOf(oldbalanceDest));
                        row.put("newbalanceDest", String.valueOf(newbalanceDest));
                        row.put("fraud", String.valueOf(fraud));
                        row.put("prob_fraud", String.valueOf(prob));
                        previewRows.add(row);
                    }
                }

                pw.flush();
            }

            // =========================
            // IMPORTANT UX FIX:
            // If nothing parsed/predicted -> show ERROR (not "completed 0")
            // =========================
            if (totalPredicted == 0) {
                request.setAttribute(
                        "error",
                        "No valid transactions were parsed. Please check your CSV format and compare it with the Sample CSV."
                );
                request.setAttribute("previewRows", previewRows);

                // Do NOT show download token or success message
            } else {
                request.setAttribute("previewRows", previewRows);
                request.setAttribute("downloadToken", token);
                request.setAttribute("okMsg",
                        "Batch prediction completed. Predicted transactions in total: " + totalPredicted + ".");
            }

        } catch (Exception ex) {
            request.setAttribute("error", ex.toString());
        }

        request.getRequestDispatcher("/batch.jsp").forward(request, response);
    }

    private Map<String, String> getOrCreateTokenMap(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<String, String> tokenMap = (Map<String, String>) session.getAttribute("BATCH_TOKEN_MAP");
        if (tokenMap == null) {
            tokenMap = new HashMap<>();
            session.setAttribute("BATCH_TOKEN_MAP", tokenMap);
        }
        return tokenMap;
    }

    private boolean looksLikeHeader(String line) {
        String low = line.toLowerCase(Locale.ROOT);
        return low.contains("step") && low.contains("type") && low.contains("amount");
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String[] splitCsvLineSimple(String line) {
        String[] parts = line.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}
