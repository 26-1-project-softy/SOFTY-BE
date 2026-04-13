package com.softy.be.auth.controller;

import com.softy.be.auth.dto.ClassCodeData;
import com.softy.be.auth.dto.KakaoLoginData;
import com.softy.be.auth.dto.KakaoLoginRequest;
import com.softy.be.auth.dto.ParentSignupRequest;
import com.softy.be.auth.dto.SignupUserData;
import com.softy.be.auth.dto.TeacherSignupRequest;
import com.softy.be.auth.service.AuthService;
import com.softy.be.auth.service.ClassCodeCreateResult;
import com.softy.be.auth.service.KakaoLoginResult;
import com.softy.be.auth.service.ParentSignupResult;
import com.softy.be.auth.service.TeacherSignupResult;
import com.softy.be.auth.service.TokenAuthService;
import com.softy.be.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenAuthService tokenAuthService;

        @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<KakaoLoginData>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        KakaoLoginResult result = authService.loginWithKakaoAccessToken(
                request == null ? null : request.kakaoAccessToken()
        );

        ApiResponse<KakaoLoginData> response = ApiResponse.of(
                true,
                200,
                "Login successful.",
                new KakaoLoginData(result.accessToken(), result.refreshToken())
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/teachers/signup")
    public ResponseEntity<ApiResponse<SignupUserData>> signupTeacher(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody TeacherSignupRequest request
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        TeacherSignupResult result = authService.signupTeacher(userId, request);

        ApiResponse<SignupUserData> response = ApiResponse.of(
                true,
                201,
                "Teacher signup completed.",
                new SignupUserData(result.userId(), result.role())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/parents/signup")
    public ResponseEntity<ApiResponse<SignupUserData>> signupParent(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody ParentSignupRequest request
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        ParentSignupResult result = authService.signupParent(userId, request);

        ApiResponse<SignupUserData> response = ApiResponse.of(
                true,
                201,
                "Parent signup completed.",
                new SignupUserData(result.userId(), result.role())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/teachers/classcode")
    public ResponseEntity<ApiResponse<ClassCodeData>> createClassCode(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        ClassCodeCreateResult result = authService.createTeacherClassCode(userId);

        ApiResponse<ClassCodeData> response = ApiResponse.of(
                true,
                201,
                "Class code issued successfully.",
                new ClassCodeData(result.classCode())
        );

        return ResponseEntity.ok(response);
    }
}


