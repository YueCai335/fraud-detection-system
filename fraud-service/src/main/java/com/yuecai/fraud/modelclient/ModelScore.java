package com.yuecai.fraud.modelclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** One scored transaction as returned by {@code POST /predict} and each entry of {@code /batch_predict}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelScore(
        int fraud,
        @JsonProperty("prob_fraud") Double probFraud,
        String reason1,
        String reason2,
        String reason3,
        double threshold) {

    public boolean isFraud() {
        return fraud == 1;
    }

    public List<String> reasons() {
        return List.of(orEmpty(reason1), orEmpty(reason2), orEmpty(reason3));
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
