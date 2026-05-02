package com.softy.be.user.service;

import java.util.List;

public record ParentSettingResult(
        Integer grade,
        Integer classNumber,
        String studentName,
        String teacherName,
        List<ParentSettingScheduleResult> schedules
) {
}
