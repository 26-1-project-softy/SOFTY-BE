package com.softy.be.user.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.UserMeData;
import com.softy.be.user.service.UserAccountService;
import com.softy.be.user.service.UserMeResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "사용자 계정", description = "내 정보 및 계정 관리 API")
public class UserController {

    private final UserAccountService userAccountService;

    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "인증된 사용자의 현재 세션 역할 기준 프로필 정보를 반환합니다."
    )
    public ResponseEntity<ApiResponse<UserMeData>> me(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();
        UserMeResult result = userAccountService.getMe(userId, principal.activeRole());

        ApiResponse<UserMeData> response = ApiResponse.of(
                true,
                200,
                "현재 사용자 정보 조회에 성공했습니다.",
                new UserMeData(result.activeRole(), result.name(), result.grade(), result.classNumber())
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "인증된 사용자의 계정과 소셜 로그인 연결 정보를 제거합니다."
    )
    public ResponseEntity<ApiResponse<Object>> withdraw(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();
        userAccountService.withdraw(userId);

        ApiResponse<Object> response = ApiResponse.of(
                true,
                200,
                "회원 탈퇴가 완료되었습니다.",
                null
        );

        return ResponseEntity.ok(response);
    }
}
