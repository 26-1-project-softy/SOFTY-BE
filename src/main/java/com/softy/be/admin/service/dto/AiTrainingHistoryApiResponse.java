package com.softy.be.admin.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("unused")
public class AiTrainingHistoryApiResponse {

    @JsonProperty("content_type")
    public String contentType;

    @JsonProperty("result_code")
    public Integer resultCode;

    @JsonProperty("result_msg")
    public String resultMessage;

    public Pagination pagination;

    public List<TrainingHistoryItem> data;

    @SuppressWarnings("unused")
    public static class Pagination {
        public Integer page;

        @JsonProperty("page_size")
        public Integer pageSize;

        @JsonProperty("total_count")
        public Integer totalCount;

        @JsonProperty("total_pages")
        public Integer totalPages;
    }

    @SuppressWarnings("unused")
    public static class TrainingHistoryItem {
        @JsonProperty("training_date")
        public String trainingDate;

        public String version;

        @JsonProperty("dataset")
        public String datasetVersion;

        @JsonProperty("f1_score")
        public Double f1Score;

        public String status;
    }
}
