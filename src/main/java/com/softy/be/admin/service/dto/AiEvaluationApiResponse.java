package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class AiEvaluationApiResponse {

    @JsonProperty("evaluation_id")
    public String evaluationId;

    @JsonProperty("result_code")
    public Integer resultCode;

    @JsonProperty("result_msg")
    public String resultMessage;

    public String version;
    public String status;
    public Double precision;
    public Double recall;

    @JsonProperty("f1_score")
    public Double f1Score;

    public Boolean passed;
}
