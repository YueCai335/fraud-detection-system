package ec.fraud.ws.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PredictRequest", namespace = "http://ws.fraud.ec/")
public class PredictRequest {

    public int step;
    public int type_code;
    public double amount;
    public double oldbalanceOrg;
    public double newbalanceOrig;
    public double oldbalanceDest;
    public double newbalanceDest;
    public double balanceDiffOrg;
    public double balanceDiffDest;

    public PredictRequest() { }
}