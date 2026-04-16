package com.softy.be.user.dto;

import java.time.LocalTime;

public record TeacherSettingScheduleData(
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
