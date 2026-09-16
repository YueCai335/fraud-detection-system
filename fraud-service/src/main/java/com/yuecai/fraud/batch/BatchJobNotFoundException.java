package com.yuecai.fraud.batch;

public class BatchJobNotFoundException extends RuntimeException {

    public BatchJobNotFoundException(String id) {
        super("No batch job " + id + " for this user");
    }
}
