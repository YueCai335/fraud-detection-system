package ec.fraud.ws.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PredictResponse", namespace = "http://ws.fraud.ec/")
public class PredictResponse {

    public int fraud;
    public double prob_fraud;

    public String reason1;
    public String reason2;
    public String reason3;

    public PredictResponse() { }

    public PredictResponse(int fraud, double prob_fraud) {
        this.fraud = fraud;
        this.prob_fraud = prob_fraud;
        this.reason1 = "";
        this.reason2 = "";
        this.reason3 = "";
    }

    public PredictResponse(int fraud, double prob_fraud, String r1, String r2, String r3) {
        this.fraud = fraud;
        this.prob_fraud = prob_fraud;
        this.reason1 = (r1 == null) ? "" : r1;
        this.reason2 = (r2 == null) ? "" : r2;
        this.reason3 = (r3 == null) ? "" : r3;
    }
}
