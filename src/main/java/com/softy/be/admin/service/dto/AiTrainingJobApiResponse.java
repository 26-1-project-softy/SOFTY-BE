package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class AiTrainingJobApiResponse {

    @JsonProperty("content_type")
    public String contentType;

    @JsonProperty("result_code")
    public Integer resultCode;

    @JsonProperty("result_msg")
    public String resultMessage;

    @JsonProperty("job_id")
    public String jobId;

    public String status;

    @JsonProperty("started_at")
    public String startedAt;

    @JsonProperty("finished_at")
    public String finishedAt;

    @JsonProperty("model_name")
    public String modelName;

    public String version;

    @JsonProperty("dataset_version")
    public String datasetVersion;

    @JsonProperty("base_version")
    public String baseVersion;

    @JsonProperty("from_date")
    public String fromDate;

    @JsonProperty("to_date")
    public String toDate;
}
