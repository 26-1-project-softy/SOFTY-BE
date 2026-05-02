package com.softy.be.user.service;

import java.time.LocalTime;

public record ParentSettingScheduleResult(
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
