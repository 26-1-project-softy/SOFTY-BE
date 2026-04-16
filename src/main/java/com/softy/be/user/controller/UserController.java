package com.softy.be.user.controller;

import com.softy.be.auth.service.TokenAuthService;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.dto.UserMeData;
import com.softy.be.user.service.UserAccountService;
import com.softy.be.user.service.UserMeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final TokenAuthService tokenAuthService;
    private final UserAccountService userAccountService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeData>> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
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
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
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
