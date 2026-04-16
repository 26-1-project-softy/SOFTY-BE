package com.softy.be.user.dto;

import java.util.List;

public record TeacherWorkHoursUpdateRequest(
        List<TeacherWorkHoursScheduleRequest> schedules
) {
}
