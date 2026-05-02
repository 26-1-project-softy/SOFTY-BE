package com.softy.be.user.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.ParentClassPreviewData;
import com.softy.be.user.dto.ParentClassPreviewRequest;
import com.softy.be.user.dto.ParentSettingData;
import com.softy.be.user.dto.ParentSettingScheduleData;
import com.softy.be.user.service.ParentClassPreviewResult;
import com.softy.be.user.service.ParentSettingResult;
import com.softy.be.user.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/parent")
@RequiredArgsConstructor
@Tag(name = "학부모 설정", description = "학부모 설정 조회 API")
public class ParentController {

    private final UserAccountService userAccountService;

    @PostMapping("/me/class/preview")
    @Operation(
            summary = "학급 변경 미리보기",
            description = "입력한 학급 코드로 변경될 학교, 학년, 반 정보를 미리 조회합니다."
    )
    public ResponseEntity<ApiResponse<ParentClassPreviewData>> previewClassChange(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody ParentClassPreviewRequest request
    ) {
        Long userId = principal.userId();
        ParentClassPreviewResult result = userAccountService.previewParentClassChange(userId, request);

        ApiResponse<ParentClassPreviewData> response = ApiResponse.of(
                true,
                200,
                "학급 코드 확인이 완료되었습니다.",
                new ParentClassPreviewData(
                        result.classCode(),
                        result.schoolName(),
                        result.grade(),
                        result.classNumber()
                )
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/setting")
    @Operation(
            summary = "학부모 설정 조회",
            description = "학부모 설정 화면에 필요한 학생, 담임, 근무시간 정보를 반환합니다."
    )
    public ResponseEntity<ApiResponse<ParentSettingData>> getSetting(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();
        ParentSettingResult result = userAccountService.getParentSetting(userId);

        List<ParentSettingScheduleData> schedules = result.schedules()
                .stream()
                .map(schedule -> new ParentSettingScheduleData(
                        schedule.dayOfWeek(),
                        schedule.startTime(),
                        schedule.endTime()
                ))
                .toList();

        ApiResponse<ParentSettingData> response = ApiResponse.of(
                true,
                200,
                "학부모 설정 정보 조회가 완료되었습니다.",
                new ParentSettingData(
                        result.grade(),
                        result.classNumber(),
                        result.studentName(),
                        result.teacherName(),
                        schedules
                )
        );

        return ResponseEntity.ok(response);
    }
}
