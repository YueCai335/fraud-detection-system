package com.yuecai.fraud.modelclient;

/**
 * Transient failure — unreachable, timed out, or a 5xx. Worth retrying.
 * (A 4xx means we sent something the model rejects; retrying would not help, so that stays
 * a plain {@link ModelServiceException}.)
 */
public class ModelServiceUnavailableException extends ModelServiceException {

    public ModelServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
