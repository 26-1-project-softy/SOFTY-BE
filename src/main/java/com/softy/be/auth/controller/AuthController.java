package com.softy.be.auth.controller;

import com.softy.be.auth.dto.ClassCodeData;
import com.softy.be.auth.dto.KakaoCodeLoginRequest;
import com.softy.be.auth.dto.KakaoLoginData;
import com.softy.be.auth.dto.KakaoLoginRequest;
import com.softy.be.auth.dto.ParentSignupRequest;
import com.softy.be.auth.dto.SignupUserData;
import com.softy.be.auth.dto.TeacherSignupRequest;
import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.auth.service.AuthService;
import com.softy.be.auth.service.KakaoLoginResult;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.user.service.ClassCodeCreateResult;
import com.softy.be.user.service.ParentSignupResult;
import com.softy.be.user.service.TeacherSignupResult;
import com.softy.be.user.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인 및 회원가입 API")
public class AuthController {

    private final AuthService authService;
    private final UserRegistrationService userRegistrationService;

    @PostMapping("/kakao/login")
    @Operation(
            summary = "카카오 액세스 토큰 로그인",
            description = "카카오 액세스 토큰으로 로그인하고 서비스 토큰을 반환합니다."
    )
    public ResponseEntity<ApiResponse<KakaoLoginData>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        KakaoLoginResult result = authService.loginWithKakaoAccessToken(
                request == null ? null : request.kakaoAccessToken()
        );

        ApiResponse<KakaoLoginData> response = ApiResponse.of(
                true,
                201,
                "로그인에 성공했습니다.",
                new KakaoLoginData(result.accessToken(), result.refreshToken(), result.registrationRequired())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/kakao/login/teacher")
    @Operation(
            summary = "교사 웹 카카오 로그인",
            description = "카카오 인가 코드를 교환해 로그인하고 서비스 토큰을 반환합니다."
    )
    public ResponseEntity<ApiResponse<KakaoLoginData>> kakaoWebLogin(@RequestBody KakaoCodeLoginRequest request) {
        KakaoLoginResult result = authService.loginWithKakaoAuthorizationCode(
                request == null ? null : request.authorizationCode(),
                request == null ? null : request.redirectUri()
        );

        ApiResponse<KakaoLoginData> response = ApiResponse.of(
                true,
                201,
                "로그인에 성공했습니다.",
                new KakaoLoginData(result.accessToken(), result.refreshToken(), result.registrationRequired())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/teachers/signup")
    @Operation(
            summary = "교사 회원가입 완료",
            description = "인증된 사용자의 교사 회원가입을 진행합니다."
    )
    public ResponseEntity<ApiResponse<SignupUserData>> signupTeacher(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody TeacherSignupRequest request
    ) {
        Long userId = principal.userId();
        TeacherSignupResult result = userRegistrationService.signupTeacher(userId, request);

        ApiResponse<SignupUserData> response = ApiResponse.of(
                true,
                201,
                "교사 회원가입이 완료되었습니다.",
                new SignupUserData(result.userId(), result.role())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/parents/signup")
    @Operation(
            summary = "학부모 회원가입 완료",
            description = "인증된 사용자의 학부모 회원가입을 진행합니다."
    )
    public ResponseEntity<ApiResponse<SignupUserData>> signupParent(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody ParentSignupRequest request
    ) {
        Long userId = principal.userId();
        ParentSignupResult result = userRegistrationService.signupParent(userId, request);

        ApiResponse<SignupUserData> response = ApiResponse.of(
                true,
                201,
                "학부모 회원가입이 완료되었습니다.",
                new SignupUserData(result.userId(), result.role())
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/teachers/classcode")
    @Operation(
            summary = "교사 학급 코드 발급",
            description = "학부모가 교사 학급에 연결할 때 사용할 학급 코드를 발급합니다."
    )
    public ResponseEntity<ApiResponse<ClassCodeData>> createClassCode(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long userId = principal.userId();
        ClassCodeCreateResult result = userRegistrationService.createTeacherClassCode(userId, principal.activeRole());

        ApiResponse<ClassCodeData> response = ApiResponse.of(
                true,
                201,
                "학급 코드 발급이 완료되었습니다.",
                new ClassCodeData(result.classCode())
        );

        return ResponseEntity.ok(response);
    }
}
