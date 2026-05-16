package com.softy.be.auth.service;

import com.softy.be.user.entity.SocialAccount;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.SocialAccountRepository;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final String DEV_KAKAO_PROVIDER = "DEV_KAKAO";
    private static final String ROLE_UNASSIGNED = "UNASSIGNED";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_PARENT = "PARENT";
    private static final String LEGACY_ROLE_USER = "USER";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtService jwtService;

    @Transactional
    public KakaoLoginResult loginWithKakaoAccessToken(String kakaoAccessToken) {
        if (isBlank(kakaoAccessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 액세스 토큰은 필수입니다.");
        }

        User user = upsertKakaoUser(kakaoAccessToken.trim());
        return createLoginResult(user, ROLE_PARENT);
    }

    @Transactional
    public KakaoLoginResult loginWithKakaoAuthorizationCode(String code, String redirectUri) {
        if (isBlank(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 인가 코드는 필수입니다.");
        }
        if (isBlank(redirectUri)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirectUri는 필수입니다.");
        }

        String kakaoAccessToken = kakaoOAuthClient.exchangeCodeForAccessToken(code.trim(), redirectUri.trim());
        User user = upsertKakaoUser(kakaoAccessToken);
        return createLoginResult(user, ROLE_TEACHER);
    }

    @Transactional
    public KakaoLoginResult loginForDev(String socialId, String role, String nickname) {
        if (isBlank(socialId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "socialId는 필수입니다.");
        }

        String normalizedRole = normalizeRoleForDev(role);
        String resolvedNickname = resolveDevNickname(socialId, nickname);

        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(DEV_KAKAO_PROVIDER, socialId.trim())
                .orElseGet(() -> createDevKakaoAccount(socialId.trim(), resolvedNickname));

        User user = socialAccount.getUser();
        user.applyDevLoginProfile(resolvedNickname, normalizedRole);

        return createLoginResult(user, normalizedRole);
    }

    private SocialAccount createKakaoAccount(KakaoUserProfile profile) {
        User user = User.createForKakao(profile.nickname());
        userRepository.save(user);

        SocialAccount socialAccount = SocialAccount.create(user, KAKAO_PROVIDER, profile.providerUserId());
        return socialAccountRepository.save(socialAccount);
    }

    private SocialAccount createDevKakaoAccount(String socialId, String nickname) {
        User user = User.createForKakao(nickname);
        userRepository.save(user);

        SocialAccount socialAccount = SocialAccount.create(user, DEV_KAKAO_PROVIDER, socialId);
        return socialAccountRepository.save(socialAccount);
    }

    private User upsertKakaoUser(String kakaoAccessToken) {
        KakaoUserProfile profile = kakaoOAuthClient.getUserProfile(kakaoAccessToken);
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(KAKAO_PROVIDER, profile.providerUserId())
                .orElseGet(() -> createKakaoAccount(profile));

        User user = socialAccount.getUser();
        if (user.getUserRoles().isEmpty() && !Objects.equals(user.getName(), profile.nickname())) {
            user.updateName(profile.nickname());
        }
        return user;
    }

    private KakaoLoginResult createLoginResult(User user, String activeRole) {
        String resolvedActiveRole = activeRole == null ? ROLE_UNASSIGNED : activeRole;
        String accessToken = jwtService.createAccessToken(user.getId(), user.getName(), resolvedActiveRole);
        String refreshToken = jwtService.createRefreshToken(user.getId(), resolvedActiveRole);
        boolean registrationRequired = isRegistrationRequiredForRole(user, resolvedActiveRole);
        return new KakaoLoginResult(accessToken, refreshToken, registrationRequired);
    }

    private boolean isRegistrationRequiredForRole(User user, String role) {
        if (role == null || ROLE_UNASSIGNED.equalsIgnoreCase(role) || LEGACY_ROLE_USER.equalsIgnoreCase(role)) {
            return true;
        }
        return !user.hasRole(role);
    }

    private String normalizeRoleForDev(String role) {
        if (isBlank(role)) {
            return ROLE_UNASSIGNED;
        }

        String normalized = role.trim().toUpperCase();
        if (ROLE_UNASSIGNED.equals(normalized) || LEGACY_ROLE_USER.equals(normalized)) {
            return ROLE_UNASSIGNED;
        }
        if (ROLE_TEACHER.equals(normalized)) {
            return ROLE_TEACHER;
        }
        if (ROLE_PARENT.equals(normalized)) {
            return ROLE_PARENT;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "role은 UNASSIGNED, TEACHER, PARENT 중 하나여야 합니다."
        );
    }

    private String resolveDevNickname(String socialId, String nickname) {
        if (!isBlank(nickname)) {
            return nickname.trim();
        }
        return "dev_" + socialId.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
