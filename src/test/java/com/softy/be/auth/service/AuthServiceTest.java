package com.softy.be.auth.service;

import com.softy.be.user.entity.SocialAccount;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.SocialAccountRepository;
import com.softy.be.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DEV_PROVIDER = "DEV_KAKAO";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_PARENT = "PARENT";
    private static final String ROLE_UNASSIGNED = "UNASSIGNED";

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginForDevAccumulatesTeacherAndParentRolesOnSameAccount() {
        User user = User.createForKakao("dev_user");
        SocialAccount socialAccount = SocialAccount.create(user, DEV_PROVIDER, "test-001");

        when(socialAccountRepository.findByProviderAndProviderUserId(DEV_PROVIDER, "test-001"))
                .thenReturn(Optional.of(socialAccount));
        when(jwtService.createAccessToken(any(), any(), eq(ROLE_TEACHER))).thenReturn("teacher-access");
        when(jwtService.createRefreshToken(any(), eq(ROLE_TEACHER))).thenReturn("teacher-refresh");
        when(jwtService.createAccessToken(any(), any(), eq(ROLE_PARENT))).thenReturn("parent-access");
        when(jwtService.createRefreshToken(any(), eq(ROLE_PARENT))).thenReturn("parent-refresh");

        KakaoLoginResult teacherLogin = authService.loginForDev("test-001", ROLE_TEACHER, "dev_user");
        KakaoLoginResult parentLogin = authService.loginForDev("test-001", ROLE_PARENT, "dev_user");

        assertThat(user.hasRole(ROLE_TEACHER)).isTrue();
        assertThat(user.hasRole(ROLE_PARENT)).isTrue();
        assertThat(teacherLogin.registrationRequired()).isFalse();
        assertThat(parentLogin.registrationRequired()).isFalse();
    }

    @Test
    void loginForDevWithUnassignedClearsExistingRoles() {
        User user = User.createForKakao("dev_user");
        user.completeTeacherSignup("dev_user");
        user.completeParentSignup("dev_user");
        SocialAccount socialAccount = SocialAccount.create(user, DEV_PROVIDER, "test-001");

        when(socialAccountRepository.findByProviderAndProviderUserId(DEV_PROVIDER, "test-001"))
                .thenReturn(Optional.of(socialAccount));
        when(jwtService.createAccessToken(any(), any(), eq(ROLE_UNASSIGNED))).thenReturn("unassigned-access");
        when(jwtService.createRefreshToken(any(), eq(ROLE_UNASSIGNED))).thenReturn("unassigned-refresh");

        KakaoLoginResult result = authService.loginForDev("test-001", ROLE_UNASSIGNED, "dev_user");

        assertThat(user.getUserRoles()).isEmpty();
        assertThat(result.registrationRequired()).isTrue();
    }
}
