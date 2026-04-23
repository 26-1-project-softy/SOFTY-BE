package com.softy.be.user.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.TeacherClassUpdateData;
import com.softy.be.user.dto.TeacherClassUpdateRequest;
import com.softy.be.user.dto.TeacherSettingData;
import com.softy.be.user.dto.TeacherSettingScheduleData;
import com.softy.be.user.dto.TeacherWorkHoursUpdateRequest;
import com.softy.be.user.service.TeacherClassUpdateResult;
import com.softy.be.user.service.TeacherSettingResult;
import com.softy.be.user.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Tag(name = "교사 설정", description = "교사 프로필 및 학급 관리 API")
public class TeacherController {

    private final UserAccountService userAccountService;

    @GetMapping("/setting")
    @Operation(
            summary = "교사 설정 조회",
            description = "교사 프로필, 학급 정보, 근무시간 스케줄을 반환합니다."
    )
    public ResponseEntity<ApiResponse<TeacherSettingData>> getSetting(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();
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
    @Operation(
            summary = "교사 학급 정보 수정",
            description = "학년과 반 정보를 수정하고 최신 학급 코드를 반환합니다."
    )
    public ResponseEntity<ApiResponse<TeacherClassUpdateData>> updateMyClass(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody TeacherClassUpdateRequest request
    ) {
        Long userId = principal.userId();
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
    @Operation(
            summary = "교사 근무시간 수정",
            description = "서비스 운영에 사용되는 주간 근무시간 스케줄을 수정합니다."
    )
    public ResponseEntity<ApiResponse<Object>> updateMyWorkHours(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody TeacherWorkHoursUpdateRequest request
    ) {
        Long userId = principal.userId();
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
