package com.yuecai.fraud.prediction;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Outcome of scoring one uploaded CSV")
public record BatchResult(
        @Schema(description = "Use with GET /api/v1/predictions/batches/{batchId}/csv") String batchId,
        int total,
        long fraudCount,
        @Schema(description = "Lines that could not be parsed, with the reason") List<String> skipped,
        List<PredictionResult> results) {

    public List<PredictionResult> preview(int n) {
        return results.subList(0, Math.min(n, results.size()));
    }
}
