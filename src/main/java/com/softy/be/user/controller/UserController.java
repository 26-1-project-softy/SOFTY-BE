package com.softy.be.user.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.UserMeData;
import com.softy.be.user.service.UserAccountService;
import com.softy.be.user.service.UserMeResult;
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
public class UserController {

    private final UserAccountService userAccountService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeData>> me(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();
        UserMeResult result = userAccountService.getMe(userId);

        ApiResponse<UserMeData> response = ApiResponse.of(
                true,
                200,
                "현재 사용자 정보 조회에 성공했습니다.",
                new UserMeData(result.role(), result.name(), result.grade(), result.classNumber())
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
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
