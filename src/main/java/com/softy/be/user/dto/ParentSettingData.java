package com.softy.be.user.dto;

import java.util.List;

public record ParentSettingData(
        Integer grade,
        Integer classNumber,
        String studentName,
        String teacherName,
        List<ParentSettingScheduleData> schedules
) {
}
