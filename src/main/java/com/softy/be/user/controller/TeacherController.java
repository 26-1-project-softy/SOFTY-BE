package com.softy.be.user.controller;

import com.softy.be.auth.service.TokenAuthService;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.TeacherClassUpdateData;
import com.softy.be.user.dto.TeacherClassUpdateRequest;
import com.softy.be.user.service.TeacherClassUpdateResult;
import com.softy.be.user.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TokenAuthService tokenAuthService;
    private final UserAccountService userAccountService;

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
}
