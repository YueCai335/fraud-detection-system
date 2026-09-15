package com.yuecai.fraud.prediction;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Public representation of a scored transaction (REST response and JSP model). */
@Schema(description = "Result of scoring one transaction")
public record PredictionResult(
        Long id,
        Integer csvRow,
        int step,
        int typeCode,
        String type,
        double amount,
        double oldbalanceOrg,
        double newbalanceOrig,
        double oldbalanceDest,
        double newbalanceDest,
        @Schema(description = "true when probFraud >= threshold") boolean fraud,
        Double probFraud,
        double threshold,
        @Schema(description = "Top-3 SHAP-derived explanations; \"No risk detected\" when not fraud") List<String> reasons,
        Instant createdAt) {

    public static PredictionResult from(Prediction p) {
        return new PredictionResult(
                p.getId(), p.getCsvRow(),
                p.getStep(), p.getTypeCode(), TransactionType.fromCode(p.getTypeCode()).name(),
                p.getAmount(), p.getOldbalanceOrg(), p.getNewbalanceOrig(),
                p.getOldbalanceDest(), p.getNewbalanceDest(),
                p.isFraud(), p.getProbFraud(), p.getThreshold(),
                List.of(p.getReason1(), p.getReason2(), p.getReason3()),
                p.getCreatedAt());
    }

    /** Probability as a whole percentage for display, e.g. 39. */
    public int probPercent() {
        return probFraud == null ? 0 : (int) Math.round(probFraud * 100);
    }
}
