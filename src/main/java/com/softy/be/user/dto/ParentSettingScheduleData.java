package com.softy.be.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record ParentSettingScheduleData(
        Short dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime
) {
}
