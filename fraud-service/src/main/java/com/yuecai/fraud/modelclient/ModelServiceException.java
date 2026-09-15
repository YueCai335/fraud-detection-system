package com.yuecai.fraud.modelclient;

/** Raised when the model service is unreachable, times out, or returns an unexpected response. */
public class ModelServiceException extends RuntimeException {

    public ModelServiceException(String message) {
        super(message);
    }

    public ModelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
