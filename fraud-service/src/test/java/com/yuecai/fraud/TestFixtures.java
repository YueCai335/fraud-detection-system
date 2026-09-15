package com.yuecai.fraud;

import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.prediction.TransactionRequest;

/**
 * Shared examples. LEGIT / FRAUD mirror model-service/tests/test_api.py so the two services'
 * tests describe the same contract.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static final TransactionRequest LEGIT =
            new TransactionRequest(1, 3, 9839.64, 170136.0, 160296.36, 0.0, 0.0);

    public static final TransactionRequest FRAUD =
            new TransactionRequest(100, 1, 10000.0, 10000.0, 0.0, 0.0, 10000.0);

    public static final ModelScore LEGIT_SCORE =
            new ModelScore(0, 0.13, "No risk detected", "N/A", "N/A", 0.25);

    public static final ModelScore FRAUD_SCORE =
            new ModelScore(1, 0.39, "Receiver balance increased sharply",
                    "High-risk transaction type (CASH_OUT)", "Sender balance dropped sharply", 0.25);

    public static final String LEGIT_JSON = """
            {"fraud":0,"prob_fraud":0.13,"reason1":"No risk detected","reason2":"N/A","reason3":"N/A","threshold":0.25}
            """;

    public static final String FRAUD_JSON = """
            {"fraud":1,"prob_fraud":0.39,"reason1":"Receiver balance increased sharply",
             "reason2":"High-risk transaction type (CASH_OUT)","reason3":"Sender balance dropped sharply","threshold":0.25}
            """;
}
