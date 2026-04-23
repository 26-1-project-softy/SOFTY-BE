package com.softy.be.auth.controller;

import com.softy.be.admin.dto.AdminLoginData;
import com.softy.be.admin.dto.AdminLoginRequest;
import com.softy.be.admin.dto.AdminRegisterData;
import com.softy.be.admin.dto.AdminRegisterRequest;
import com.softy.be.admin.service.AdminAuthService;
import com.softy.be.admin.service.AdminLoginResult;
import com.softy.be.admin.service.AdminRegisterResult;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
@Tag(name = "관리자 인증", description = "관리자 인증 및 계정 등록 API")
public class AdminAuthController {

    private static final String PROVISION_HEADER = "X-Admin-Provision-Key";

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(
            summary = "관리자 로그인",
            description = "관리자 계정을 인증하고 액세스 토큰과 리프레시 토큰을 반환합니다."
    )
    public ResponseEntity<ApiResponse<AdminLoginData>> login(@RequestBody AdminLoginRequest request) {
        AdminLoginResult result = adminAuthService.login(request);

        ApiResponse<AdminLoginData> response = ApiResponse.of(
                true,
                200,
                "관리자 로그인에 성공했습니다.",
                new AdminLoginData(result.accessToken(), result.refreshToken())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "관리자 계정 등록",
            description = "새 관리자 계정을 생성합니다. 등록 시 X-Admin-Provision-Key 헤더가 필요합니다."
    )
    public ResponseEntity<ApiResponse<AdminRegisterData>> register(
            @RequestHeader(value = PROVISION_HEADER, required = false) String provisionKey,
            @RequestBody AdminRegisterRequest request
    ) {
        AdminRegisterResult result = adminAuthService.register(request, provisionKey);

        ApiResponse<AdminRegisterData> response = ApiResponse.of(
                true,
                201,
                "관리자 계정이 생성되었습니다.",
                new AdminRegisterData(result.userId(), result.role(), result.loginId())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
