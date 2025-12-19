
package ec.fraud.soapclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>PredictRequest complex type\u7684 Java \u7c7b\u3002
 * 
 * <p>\u4ee5\u4e0b\u6a21\u5f0f\u7247\u6bb5\u6307\u5b9a\u5305\u542b\u5728\u6b64\u7c7b\u4e2d\u7684\u9884\u671f\u5185\u5bb9\u3002
 * 
 * <pre>
 * &lt;complexType name="PredictRequest">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="step" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="type_code" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="amount" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="oldbalanceOrg" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="newbalanceOrig" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="oldbalanceDest" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="newbalanceDest" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="balanceDiffOrg" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="balanceDiffDest" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PredictRequest", propOrder = {
    "step",
    "typeCode",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
    "balanceDiffOrg",
    "balanceDiffDest"
})
public class PredictRequest {

    protected int step;
    @XmlElement(name = "type_code")
    protected int typeCode;
    protected double amount;
    protected double oldbalanceOrg;
    protected double newbalanceOrig;
    protected double oldbalanceDest;
    protected double newbalanceDest;
    protected double balanceDiffOrg;
    protected double balanceDiffDest;

    /**
     * \u83b7\u53d6step\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public int getStep() {
        return step;
    }

    /**
     * \u8bbe\u7f6estep\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setStep(int value) {
        this.step = value;
    }

    /**
     * \u83b7\u53d6typeCode\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public int getTypeCode() {
        return typeCode;
    }

    /**
     * \u8bbe\u7f6etypeCode\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setTypeCode(int value) {
        this.typeCode = value;
    }

    /**
     * \u83b7\u53d6amount\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getAmount() {
        return amount;
    }

    /**
     * \u8bbe\u7f6eamount\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setAmount(double value) {
        this.amount = value;
    }

    /**
     * \u83b7\u53d6oldbalanceOrg\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getOldbalanceOrg() {
        return oldbalanceOrg;
    }

    /**
     * \u8bbe\u7f6eoldbalanceOrg\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setOldbalanceOrg(double value) {
        this.oldbalanceOrg = value;
    }

    /**
     * \u83b7\u53d6newbalanceOrig\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getNewbalanceOrig() {
        return newbalanceOrig;
    }

    /**
     * \u8bbe\u7f6enewbalanceOrig\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setNewbalanceOrig(double value) {
        this.newbalanceOrig = value;
    }

    /**
     * \u83b7\u53d6oldbalanceDest\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getOldbalanceDest() {
        return oldbalanceDest;
    }

    /**
     * \u8bbe\u7f6eoldbalanceDest\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setOldbalanceDest(double value) {
        this.oldbalanceDest = value;
    }

    /**
     * \u83b7\u53d6newbalanceDest\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getNewbalanceDest() {
        return newbalanceDest;
    }

    /**
     * \u8bbe\u7f6enewbalanceDest\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setNewbalanceDest(double value) {
        this.newbalanceDest = value;
    }

    /**
     * \u83b7\u53d6balanceDiffOrg\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getBalanceDiffOrg() {
        return balanceDiffOrg;
    }

    /**
     * \u8bbe\u7f6ebalanceDiffOrg\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setBalanceDiffOrg(double value) {
        this.balanceDiffOrg = value;
    }

    /**
     * \u83b7\u53d6balanceDiffDest\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getBalanceDiffDest() {
        return balanceDiffDest;
    }

    /**
     * \u8bbe\u7f6ebalanceDiffDest\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setBalanceDiffDest(double value) {
        this.balanceDiffDest = value;
    }

}
