
package ec.fraud.soapclient;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ec.fraud.soapclient package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Predict_QNAME = new QName("http://ws.fraud.ec/", "predict");
    private final static QName _PredictResponse_QNAME = new QName("http://ws.fraud.ec/", "predictResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ec.fraud.soapclient
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Predict }
     * 
     */
    public Predict createPredict() {
        return new Predict();
    }

    /**
     * Create an instance of {@link PredictResponse }
     * 
     */
    public PredictResponse createPredictResponse() {
        return new PredictResponse();
    }

    /**
     * Create an instance of {@link PredictResponse2 }
     * 
     */
    public PredictResponse2 createPredictResponse2() {
        return new PredictResponse2();
    }

    /**
     * Create an instance of {@link PredictRequest }
     * 
     */
    public PredictRequest createPredictRequest() {
        return new PredictRequest();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Predict }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.fraud.ec/", name = "predict")
    public JAXBElement<Predict> createPredict(Predict value) {
        return new JAXBElement<Predict>(_Predict_QNAME, Predict.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PredictResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.fraud.ec/", name = "predictResponse")
    public JAXBElement<PredictResponse> createPredictResponse(PredictResponse value) {
        return new JAXBElement<PredictResponse>(_PredictResponse_QNAME, PredictResponse.class, null, value);
    }

}
