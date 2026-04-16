package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminLoginRequest;
import com.softy.be.admin.dto.AdminRegisterRequest;
import com.softy.be.auth.service.JwtService;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${admin.provision-key}")
    private String provisionKey;

    @Transactional(readOnly = true)
    public AdminLoginResult login(AdminLoginRequest request) {
        validateLoginRequest(request);

        User user = userRepository.findByLoginId(request.loginId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다"));

        if (!ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 계정만 로그인할 수 있습니다");
        }

        if (!passwordEncoder.matches(request.password(), user.getPw())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다");
        }

        String accessToken = jwtService.createAccessToken(user.getId(), user.getName(), user.getRole());
        String refreshToken = jwtService.createRefreshToken(user.getId(), user.getRole());

        return new AdminLoginResult(accessToken, refreshToken);
    }

    @Transactional
    public AdminRegisterResult register(AdminRegisterRequest request, String requestProvisionKey) {
        validateProvisionKey(requestProvisionKey);
        validateRegisterRequest(request);

        String loginId = request.loginId().trim();
        if (userRepository.existsByLoginId(loginId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 관리자 아이디입니다");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User admin = User.createAdmin(request.name().trim(), loginId, encodedPassword);
        User saved = userRepository.save(admin);

        return new AdminRegisterResult(saved.getId(), saved.getRole(), saved.getLoginId());
    }

    private void validateLoginRequest(AdminLoginRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다");
        }
        if (isBlank(request.loginId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 아이디는 필수입니다");
        }
        if (isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 필수입니다");
        }
    }

    private void validateRegisterRequest(AdminRegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다");
        }
        if (isBlank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 이름은 필수입니다");
        }
        if (isBlank(request.loginId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 아이디는 필수입니다");
        }
        if (isBlank(request.password()) || request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다");
        }
    }

    private void validateProvisionKey(String requestProvisionKey) {
        if (isBlank(provisionKey)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "관리자 생성 키가 설정되지 않았습니다");
        }
        if (isBlank(requestProvisionKey) || !provisionKey.equals(requestProvisionKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 생성 키가 일치하지 않습니다");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
