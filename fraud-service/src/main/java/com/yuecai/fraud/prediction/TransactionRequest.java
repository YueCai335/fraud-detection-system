package com.yuecai.fraud.prediction;

import com.yuecai.fraud.modelclient.ModelFeatures;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** A single transaction to score — the same seven raw PaySim columns the old JSP form collected. */
@Schema(description = "One PaySim-style transaction")
public record TransactionRequest(
        @Schema(description = "Hour of the simulation (1–744)", example = "100")
        @NotNull @Min(0) Integer step,

        @Schema(description = "0=CASH_IN 1=CASH_OUT 2=DEBIT 3=PAYMENT 4=TRANSFER", example = "1")
        @NotNull @Min(0) @Max(4) Integer typeCode,

        @Schema(example = "10000.0") @NotNull @PositiveOrZero Double amount,
        @Schema(example = "10000.0") @NotNull @PositiveOrZero Double oldbalanceOrg,
        @Schema(example = "0.0") @NotNull @PositiveOrZero Double newbalanceOrig,
        @Schema(example = "0.0") @NotNull @PositiveOrZero Double oldbalanceDest,
        @Schema(example = "10000.0") @NotNull @PositiveOrZero Double newbalanceDest) {

    public ModelFeatures toFeatures() {
        return ModelFeatures.of(step, typeCode, amount, oldbalanceOrg, newbalanceOrig, oldbalanceDest, newbalanceDest);
    }
}
