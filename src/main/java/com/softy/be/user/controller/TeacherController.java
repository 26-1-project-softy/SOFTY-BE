package com.softy.be.user.controller;

import com.softy.be.auth.service.TokenAuthService;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.TeacherClassUpdateData;
import com.softy.be.user.dto.TeacherClassUpdateRequest;
import com.softy.be.user.dto.TeacherSettingData;
import com.softy.be.user.dto.TeacherSettingScheduleData;
import com.softy.be.user.dto.TeacherWorkHoursUpdateRequest;
import com.softy.be.user.service.TeacherClassUpdateResult;
import com.softy.be.user.service.TeacherSettingResult;
import com.softy.be.user.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TokenAuthService tokenAuthService;
    private final UserAccountService userAccountService;

    @GetMapping("/setting")
    public ResponseEntity<ApiResponse<TeacherSettingData>> getSetting(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        TeacherSettingResult result = userAccountService.getTeacherSetting(userId);

        List<TeacherSettingScheduleData> schedules = result.schedules()
                .stream()
                .map(schedule -> new TeacherSettingScheduleData(
                        schedule.dayOfWeek(),
                        schedule.startTime(),
                        schedule.endTime()
                ))
                .toList();

        ApiResponse<TeacherSettingData> response = ApiResponse.of(
                true,
                200,
                "교사 프로필 조회가 완료되었습니다.",
                new TeacherSettingData(
                        result.grade(),
                        result.classNumber(),
                        result.schoolName(),
                        result.classCode(),
                        result.teacherName(),
                        schedules
                )
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/class")
    public ResponseEntity<ApiResponse<TeacherClassUpdateData>> updateMyClass(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody TeacherClassUpdateRequest request
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        TeacherClassUpdateResult result = userAccountService.updateTeacherClass(userId, request);

        ApiResponse<TeacherClassUpdateData> response = ApiResponse.of(
                true,
                200,
                "학급이 변경되었습니다.",
                new TeacherClassUpdateData(result.classCode())
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/work-hours")
    public ResponseEntity<ApiResponse<Object>> updateMyWorkHours(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody TeacherWorkHoursUpdateRequest request
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        userAccountService.updateTeacherWorkHours(userId, request);

        ApiResponse<Object> response = ApiResponse.of(
                true,
                200,
                "근무시간이 저장되었습니다.",
                null
        );

        return ResponseEntity.ok(response);
    }
}
