
package ec.fraud.soapclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>PredictResponse complex type\u7684 Java \u7c7b\u3002
 * 
 * <p>\u4ee5\u4e0b\u6a21\u5f0f\u7247\u6bb5\u6307\u5b9a\u5305\u542b\u5728\u6b64\u7c7b\u4e2d\u7684\u9884\u671f\u5185\u5bb9\u3002
 * 
 * <pre>
 * &lt;complexType name="PredictResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fraud" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="prob_fraud" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="reason1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="reason2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="reason3" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PredictResponse", propOrder = {
    "fraud",
    "probFraud",
    "reason1",
    "reason2",
    "reason3"
})
public class PredictResponse2 {

    protected int fraud;
    @XmlElement(name = "prob_fraud")
    protected double probFraud;
    protected String reason1;
    protected String reason2;
    protected String reason3;

    /**
     * \u83b7\u53d6fraud\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public int getFraud() {
        return fraud;
    }

    /**
     * \u8bbe\u7f6efraud\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setFraud(int value) {
        this.fraud = value;
    }

    /**
     * \u83b7\u53d6probFraud\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public double getProbFraud() {
        return probFraud;
    }

    /**
     * \u8bbe\u7f6eprobFraud\u5c5e\u6027\u7684\u503c\u3002
     * 
     */
    public void setProbFraud(double value) {
        this.probFraud = value;
    }

    /**
     * \u83b7\u53d6reason1\u5c5e\u6027\u7684\u503c\u3002
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReason1() {
        return reason1;
    }

    /**
     * \u8bbe\u7f6ereason1\u5c5e\u6027\u7684\u503c\u3002
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReason1(String value) {
        this.reason1 = value;
    }

    /**
     * \u83b7\u53d6reason2\u5c5e\u6027\u7684\u503c\u3002
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReason2() {
        return reason2;
    }

    /**
     * \u8bbe\u7f6ereason2\u5c5e\u6027\u7684\u503c\u3002
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReason2(String value) {
        this.reason2 = value;
    }

    /**
     * \u83b7\u53d6reason3\u5c5e\u6027\u7684\u503c\u3002
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReason3() {
        return reason3;
    }

    /**
     * \u8bbe\u7f6ereason3\u5c5e\u6027\u7684\u503c\u3002
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReason3(String value) {
        this.reason3 = value;
    }

}
