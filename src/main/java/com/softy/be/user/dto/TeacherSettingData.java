package com.softy.be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TeacherSettingData(
        Integer grade,
        @JsonProperty("class") Integer classNumber,
        String schoolName,
        String classCode,
        String teacherName,
        List<TeacherSettingScheduleData> schedules
) {
}
