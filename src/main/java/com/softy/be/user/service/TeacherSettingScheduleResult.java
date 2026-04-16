package com.softy.be.user.service;

import java.time.LocalTime;

public record TeacherSettingScheduleResult(
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
