package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiEvaluationRerunApiRequest(
        String version,
        @JsonProperty("dataset_version")
        String datasetVersion
) {
}
