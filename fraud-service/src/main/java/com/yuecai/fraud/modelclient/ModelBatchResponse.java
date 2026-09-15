package com.yuecai.fraud.modelclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBatchResponse(List<ModelScore> results) {
}
