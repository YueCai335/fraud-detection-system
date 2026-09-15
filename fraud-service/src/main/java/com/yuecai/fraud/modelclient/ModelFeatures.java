package com.yuecai.fraud.modelclient;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The 9-feature vector the model expects. Field names are the model service's
 * JSON contract (snake/camel mix inherited from the PaySim column names).
 */
public record ModelFeatures(
        int step,
        @JsonProperty("type_code") int typeCode,
        double amount,
        double oldbalanceOrg,
        double newbalanceOrig,
        double oldbalanceDest,
        double newbalanceDest,
        double balanceDiffOrg,
        double balanceDiffDest) {

    /**
     * Derives the two engineered features exactly as the legacy SOAP client did:
     * balanceDiffOrg = old - new (sender), balanceDiffDest = new - old (receiver).
     */
    public static ModelFeatures of(int step, int typeCode, double amount,
                                   double oldbalanceOrg, double newbalanceOrig,
                                   double oldbalanceDest, double newbalanceDest) {
        return new ModelFeatures(step, typeCode, amount,
                oldbalanceOrg, newbalanceOrig, oldbalanceDest, newbalanceDest,
                oldbalanceOrg - newbalanceOrig,
                newbalanceDest - oldbalanceDest);
    }
}
