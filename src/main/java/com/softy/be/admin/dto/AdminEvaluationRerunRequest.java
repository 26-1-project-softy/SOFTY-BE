package com.softy.be.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminEvaluationRerunRequest(
        String version,
        @JsonProperty("datasetVersion")
        String datasetVersion
) {
}
