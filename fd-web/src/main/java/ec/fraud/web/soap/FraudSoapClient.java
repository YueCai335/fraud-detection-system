package ec.fraud.web.soap;

import ec.fraud.soapclient.FraudService;
import ec.fraud.soapclient.FraudService_Service;
import ec.fraud.soapclient.PredictRequest;

import ec.fraud.soapclient.PredictResponse2;

import java.net.URL;

public class FraudSoapClient {

    private static final String WSDL_URL =
            "http://localhost:8080/fraud-soap/FraudService?wsdl";

    public PredictResponse2 predict(
            int step,
            int typeCode,
            double amount,
            double oldbalanceOrg,
            double newbalanceOrig,
            double oldbalanceDest,
            double newbalanceDest
    ) throws Exception {

        double balanceDiffOrg = oldbalanceOrg - newbalanceOrig;
        double balanceDiffDest = newbalanceDest - oldbalanceDest;

        URL wsdl = new URL(WSDL_URL);
        FraudService_Service service = new FraudService_Service(wsdl);
        FraudService port = service.getFraudServicePort();

        PredictRequest req = new PredictRequest();
        req.setStep(step);
        req.setTypeCode(typeCode);             // type_code -> setTypeCode (wsimport standard)
        req.setAmount(amount);
        req.setOldbalanceOrg(oldbalanceOrg);
        req.setNewbalanceOrig(newbalanceOrig);
        req.setOldbalanceDest(oldbalanceDest);
        req.setNewbalanceDest(newbalanceDest);
        req.setBalanceDiffOrg(balanceDiffOrg);
        req.setBalanceDiffDest(balanceDiffDest);

        // SOAP call returns wrapper PredictResponse, the actual result is inside getReturn()
	PredictResponse2 result = port.predict(req);
	return result;
    }
}
