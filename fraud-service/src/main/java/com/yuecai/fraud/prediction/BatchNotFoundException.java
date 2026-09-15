package com.yuecai.fraud.prediction;

public class BatchNotFoundException extends RuntimeException {

    public BatchNotFoundException(String batchId) {
        super("No batch " + batchId + " for this user");
    }
}
