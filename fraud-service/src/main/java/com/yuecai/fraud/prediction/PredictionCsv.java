package com.yuecai.fraud.prediction;

import java.util.List;

/** Result CSV format shared by the synchronous batch endpoint and asynchronous batch jobs. */
public final class PredictionCsv {

    public static final String HEADER =
            "csv_row,step,type_code,type,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest,"
            + "fraud,prob_fraud,reason1,reason2,reason3";

    private PredictionCsv() {}

    public static String write(List<PredictionResult> rows) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (PredictionResult r : rows) {
            sb.append(r.csvRow()).append(',')
              .append(r.step()).append(',')
              .append(r.typeCode()).append(',')
              .append(r.type()).append(',')
              .append(r.amount()).append(',')
              .append(r.oldbalanceOrg()).append(',')
              .append(r.newbalanceOrig()).append(',')
              .append(r.oldbalanceDest()).append(',')
              .append(r.newbalanceDest()).append(',')
              .append(r.fraud() ? 1 : 0).append(',')
              .append(r.probFraud() == null ? "" : r.probFraud()).append(',')
              .append(quote(r.reasons().get(0))).append(',')
              .append(quote(r.reasons().get(1))).append(',')
              .append(quote(r.reasons().get(2))).append('\n');
        }
        return sb.toString();
    }

    /** Reason texts contain commas and parentheses. */
    static String quote(String s) {
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
