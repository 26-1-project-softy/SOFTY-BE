package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("unused")
public class AiTokenUsageApiResponse {

    @JsonProperty("content_type")
    public String contentType;

    @JsonProperty("result_code")
    public Integer resultCode;

    @JsonProperty("result_msg")
    public String resultMessage;

    @JsonProperty("total_usage")
    public AiTokenUsageSummary totalUsage;

    public List<AiTokenUsageDetail> details;

    @SuppressWarnings("unused")
    public static class AiTokenUsageSummary {
        @JsonProperty("input_tokens")
        public Integer inputTokens;

        @JsonProperty("output_tokens")
        public Integer outputTokens;

        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }

    @SuppressWarnings("unused")
    public static class AiTokenUsageDetail {
        public String endpoint;

        @JsonProperty("input_tokens")
        public Integer inputTokens;

        @JsonProperty("output_tokens")
        public Integer outputTokens;

        @JsonProperty("total_tokens")
        public Integer totalTokens;
    }
}
