package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class AiRetrainingJobApiResponse {

    @JsonProperty("content_type")
    public String contentType;

    @JsonProperty("result_code")
    public Integer resultCode;

    @JsonProperty("result_msg")
    public String resultMessage;

    @JsonProperty("job_id")
    public String jobId;

    public String status;

    @JsonProperty("progress_percent")
    public Integer progressPercent;
}
