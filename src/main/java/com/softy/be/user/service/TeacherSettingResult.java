package com.softy.be.user.service;

import java.util.List;

public record TeacherSettingResult(
        Integer grade,
        Integer classNumber,
        String schoolName,
        String classCode,
        String teacherName,
        List<TeacherSettingScheduleResult> schedules
) {
}
