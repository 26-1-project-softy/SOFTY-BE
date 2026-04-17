package com.softy.be.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

public record TeacherWorkHoursScheduleRequest(
        @Schema(example = "3", description = "요일(1=월, 7=일)")
        Short dayOfWeek,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "09:30:00", description = "근무 시작 시간")
        LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", format = "time", example = "16:00:00", description = "근무 종료 시간")
        LocalTime endTime
) {
}
