package ec.fraud.ws;

import com.google.gson.Gson;
import ec.fraud.ws.dto.FlaskPredictResult;
import ec.fraud.ws.dto.PredictRequest;
import ec.fraud.ws.dto.PredictResponse;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@WebService(
        serviceName = "FraudService",
        targetNamespace = "http://ws.fraud.ec/"
)
@BindingType(SOAPBinding.SOAP11HTTP_BINDING)
public class FraudService {

    // Flask endpoint (same machine as WildFly -> OK)
    private static final String FLASK_URL = "http://127.0.0.1:5000/predict";

    private final Gson gson = new Gson();

    @WebMethod
    public PredictResponse predict(PredictRequest req) {

        try {
            String json = buildJson(req);
            String respJson = postJson(FLASK_URL, json);

            // Parse Flask JSON response
            FlaskPredictResult fr = gson.fromJson(respJson, FlaskPredictResult.class);

            // Safety checks (lab-friendly)
            int fraud = (fr == null) ? -1 : fr.fraud;
            double prob = (fr == null) ? -1.0 : fr.prob_fraud;

            String r1 = (fr == null || fr.reason1 == null) ? "" : fr.reason1;
            String r2 = (fr == null || fr.reason2 == null) ? "" : fr.reason2;
            String r3 = (fr == null || fr.reason3 == null) ? "" : fr.reason3;

            return new PredictResponse(fraud, prob, r1, r2, r3);

        } catch (Exception ex) {
            // lab-friendly fallback
            return new PredictResponse(-1, -1.0, "ERROR: " + ex.getMessage(), "", "");
        }
    }

    private String buildJson(PredictRequest r) {
        return "{"
                + "\"step\":" + r.step + ","
                + "\"type_code\":" + r.type_code + ","
                + "\"amount\":" + r.amount + ","
                + "\"oldbalanceOrg\":" + r.oldbalanceOrg + ","
                + "\"newbalanceOrig\":" + r.newbalanceOrig + ","
                + "\"oldbalanceDest\":" + r.oldbalanceDest + ","
                + "\"newbalanceDest\":" + r.newbalanceDest + ","
                + "\"balanceDiffOrg\":" + r.balanceDiffOrg + ","
                + "\"balanceDiffDest\":" + r.balanceDiffDest
                + "}";
    }

    private String postJson(String endpoint, String jsonBody) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        "UTF-8"
                )
        );

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        return sb.toString();
    }
}
