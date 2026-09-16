package com.yuecai.fraud.prediction;

/** PaySim transaction types in the label-encoded order the model was trained with. */
public enum TransactionType {
    CASH_IN(0), CASH_OUT(1), DEBIT(2), PAYMENT(3), TRANSFER(4);

    private final int code;

    TransactionType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static TransactionType fromCode(int code) {
        for (TransactionType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown type_code " + code);
    }
}
