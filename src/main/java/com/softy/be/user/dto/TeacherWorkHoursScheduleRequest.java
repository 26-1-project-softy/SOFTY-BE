package com.softy.be.user.dto;

import java.time.LocalTime;

public record TeacherWorkHoursScheduleRequest(
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
