package com.yuecai.fraud.prediction;

import java.util.List;

/** The uploaded CSV contained no parseable transaction rows. */
public class EmptyBatchException extends RuntimeException {

    private final List<String> skipped;

    public EmptyBatchException(List<String> skipped) {
        super("No valid transactions were parsed. Expected columns: " + CsvTransactionParser.EXPECTED_HEADER);
        this.skipped = skipped;
    }

    public List<String> getSkipped() {
        return skipped;
    }
}
