package com.softy.be.auth.controller;

import com.softy.be.auth.dto.KakaoLoginData;
import com.softy.be.auth.service.AuthService;
import com.softy.be.auth.service.KakaoLoginResult;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Profile("local")
@Tag(name = "개발용 인증", description = "로컬 환경 전용 개발 인증 API")
public class DevAuthController {

    private final AuthService authService;

    @PostMapping("/dev-login")
    @Operation(
            summary = "개발용 로그인 (local 전용)",
            description = "외부 OAuth 절차 없이 로컬 개발용 로그인 토큰을 발급합니다."
    )
    public ResponseEntity<ApiResponse<KakaoLoginData>> devLogin(
            @RequestParam("socialId") String socialId,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "nickname", required = false) String nickname
    ) {
        KakaoLoginResult result = authService.loginForDev(socialId, role, nickname);

        ApiResponse<KakaoLoginData> response = ApiResponse.of(
                true,
                201,
                "Dev login succeeded.",
                new KakaoLoginData(result.accessToken(), result.refreshToken(), result.registrationRequired())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
