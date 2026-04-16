package com.softy.be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TeacherClassUpdateRequest(
        String schoolName,
        Integer grade,
        @JsonProperty("class") Integer classNumber
) {
}
