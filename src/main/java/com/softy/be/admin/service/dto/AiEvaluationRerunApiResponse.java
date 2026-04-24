package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class AiEvaluationRerunApiResponse {

    @JsonProperty("content_type")
    public String contentType;

    @JsonProperty("result_code")
    public Integer resultCode;

    @JsonProperty("result_msg")
    public String resultMessage;

    @JsonProperty("evaluation_id")
    public String evaluationId;

    public String status;
}
