package com.softy.be.auth.service;

import com.softy.be.auth.dto.ParentSignupRequest;
import com.softy.be.auth.dto.TeacherSignupRequest;
import com.softy.be.domain.school.ClassCode;
import com.softy.be.domain.school.Classroom;
import com.softy.be.domain.school.ParentStudent;
import com.softy.be.domain.school.School;
import com.softy.be.domain.user.SocialAccount;
import com.softy.be.domain.school.Student;
import com.softy.be.domain.user.User;
import com.softy.be.repository.school.ClassCodeRepository;
import com.softy.be.repository.school.ClassroomRepository;
import com.softy.be.repository.school.ParentStudentRepository;
import com.softy.be.repository.school.SchoolRepository;
import com.softy.be.repository.user.SocialAccountRepository;
import com.softy.be.repository.school.StudentRepository;
import com.softy.be.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final String ROLE_UNASSIGNED = "UNASSIGNED";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_PARENT = "PARENT";
    private static final String LEGACY_ROLE_USER = "USER";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SchoolRepository schoolRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassCodeRepository classCodeRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final JwtService jwtService;

    @Transactional
    public KakaoLoginResult loginWithKakaoAccessToken(String kakaoAccessToken) {
        if (isBlank(kakaoAccessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kakaoAccessToken is required");
        }

        User user = upsertKakaoUser(kakaoAccessToken.trim());
        String accessToken = jwtService.createAccessToken(user.getId(), user.getName(), user.getRole());
        String refreshToken = jwtService.createRefreshToken(user.getId(), user.getRole());
        boolean registrationRequired = isRegistrationRequired(user.getRole());
        return new KakaoLoginResult(accessToken, refreshToken, registrationRequired);
    }

    @Transactional
    public TeacherSignupResult signupTeacher(Long authenticatedUserId, TeacherSignupRequest request) {
        validateTeacherSignupRequest(request);

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!isRegistrationRequired(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 회원가입이 완료된 사용자입니다");
        }

        School school = schoolRepository.findByName(request.schoolName().trim())
                .orElseGet(() -> schoolRepository.save(School.create(request.schoolName().trim())));

        Classroom classroom = classroomRepository.save(
                Classroom.create(request.grade(), request.classNumber(), school, user)
        );

        String code = generateUniqueClassCode();
        classCodeRepository.save(ClassCode.create(code, classroom));

        user.completeTeacherSignup(request.teacherName().trim());
        return new TeacherSignupResult(user.getId(), user.getRole(), code);
    }

    @Transactional
    public ClassCodeCreateResult createTeacherClassCode(Long authenticatedUserId) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!ROLE_TEACHER.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 계정만 클래스 코드를 생성할 수 있습니다");
        }

        Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "생성할 학급 정보를 찾을 수 없습니다"));

        String code = generateUniqueClassCode();
        classCodeRepository.save(ClassCode.create(code, classroom));
        return new ClassCodeCreateResult(code);
    }

    @Transactional
    public ParentSignupResult signupParent(Long authenticatedUserId, ParentSignupRequest request) {
        validateParentSignupRequest(request);

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!isRegistrationRequired(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 회원가입이 완료된 사용자입니다");
        }

        ClassCode classCode = classCodeRepository.findFirstByCodeOrderByIdDesc(request.classCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효한 학급 코드를 찾을 수 없습니다"));

        Student student = studentRepository.save(
                Student.create(
                        request.studentName().trim(),
                        request.studentBirthday(),
                        request.studentGender().trim().toUpperCase(),
                        classCode.getClassroom()
                )
        );
        parentStudentRepository.save(ParentStudent.create(user, student));

        user.completeParentSignup(request.parentName().trim());
        return new ParentSignupResult(user.getId(), user.getRole());
    }

    private SocialAccount createKakaoAccount(KakaoUserProfile profile) {
        User user = User.createForKakao(profile.nickname());
        userRepository.save(user);

        SocialAccount socialAccount = SocialAccount.create(user, KAKAO_PROVIDER, profile.providerUserId());
        return socialAccountRepository.save(socialAccount);
    }

    private User upsertKakaoUser(String kakaoAccessToken) {
        KakaoUserProfile profile = kakaoOAuthClient.getUserProfile(kakaoAccessToken);
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(KAKAO_PROVIDER, profile.providerUserId())
                .orElseGet(() -> createKakaoAccount(profile));

        User user = socialAccount.getUser();
        if (!Objects.equals(user.getName(), profile.nickname())) {
            user.updateName(profile.nickname());
        }
        return user;
    }

    private boolean isRegistrationRequired(String role) {
        return role == null || ROLE_UNASSIGNED.equalsIgnoreCase(role) || LEGACY_ROLE_USER.equalsIgnoreCase(role);
    }

    private void validateTeacherSignupRequest(TeacherSignupRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다");
        }
        if (isBlank(request.teacherName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "교사 이름은 필수입니다");
        }
        if (isBlank(request.schoolName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학교 이름은 필수입니다");
        }
        if (request.grade() == null || request.grade() < 1 || request.grade() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학년은 1~6 사이여야 합니다");
        }
        if (request.classNumber() == null || request.classNumber() < 1 || request.classNumber() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반 번호는 1~30 사이여야 합니다");
        }
    }

    private void validateParentSignupRequest(ParentSignupRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다");
        }
        if (isBlank(request.parentName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학부모 이름은 필수입니다");
        }
        if (isBlank(request.studentName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 이름은 필수입니다");
        }
        if (request.studentBirthday() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 생일은 필수입니다");
        }
        if (isBlank(request.studentGender())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 성별는 필수입니다");
        }
        String gender = request.studentGender().trim().toUpperCase();
        if (!"M".equals(gender) && !"F".equals(gender)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 성별는 M 또는 F로 입력해야 합니다");
        }
        if (isBlank(request.classCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학급 코드는 필수입니다");
        }
    }

    private String generateUniqueClassCode() {
        for (int i = 0; i < 20; i++) {
            String candidate = randomCodeChunk(3) + "-" + randomCodeChunk(3);
            if (!classCodeRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "고유한 학급 코드를 생성하지 못했습니다");
    }

    private String randomCodeChunk(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int index = RANDOM.nextInt(CODE_CHARS.length());
            builder.append(CODE_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
