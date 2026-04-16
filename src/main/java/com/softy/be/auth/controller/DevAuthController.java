package com.softy.be.auth.controller;

import com.softy.be.auth.dto.KakaoLoginData;
import com.softy.be.auth.service.AuthService;
import com.softy.be.auth.service.KakaoLoginResult;
import com.softy.be.common.api.ApiResponse;
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
public class DevAuthController {

    private final AuthService authService;

    @PostMapping("/dev-login")
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
