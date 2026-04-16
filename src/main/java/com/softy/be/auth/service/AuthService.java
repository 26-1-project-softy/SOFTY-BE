package com.softy.be.auth.service;

import com.softy.be.auth.dto.ParentSignupRequest;
import com.softy.be.auth.dto.TeacherSignupRequest;
import com.softy.be.school.entity.ClassCode;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.entity.School;
import com.softy.be.school.entity.Student;
import com.softy.be.user.entity.SocialAccount;
import com.softy.be.user.entity.User;
import com.softy.be.school.repository.ClassCodeRepository;
import com.softy.be.school.repository.ClassroomRepository;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.SchoolRepository;
import com.softy.be.school.repository.StudentRepository;
import com.softy.be.school.service.ClassCodeService;
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
    private static final String ROLE_UNASSIGNED = "UNASSIGNED";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_PARENT = "PARENT";
    private static final String LEGACY_ROLE_USER = "USER";
    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SchoolRepository schoolRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassCodeRepository classCodeRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final JwtService jwtService;
    private final ClassCodeService classCodeService;

    @Transactional
    public KakaoLoginResult loginWithKakaoAccessToken(String kakaoAccessToken) {
        if (isBlank(kakaoAccessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 액세스 토큰은 필수입니다");
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

        String code = classCodeService.createClassCodeForClassroom(classroom);

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

        String code = classCodeService.createClassCodeForClassroom(classroom);
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

        String studentName = request.studentName().trim();
        String studentGender = request.studentGender().trim().toUpperCase();
        Long classroomId = classCode.getClassroom().getId();

        Student student = studentRepository
                .findFirstByClassroomIdAndNameAndBirthdayAndGenderOrderByIdDesc(
                        classroomId,
                        studentName,
                        request.studentBirthday(),
                        studentGender
                )
                .orElseGet(() -> studentRepository.save(
                        Student.create(
                                studentName,
                                request.studentBirthday(),
                                studentGender,
                                classCode.getClassroom()
                        )
                ));

        if (!parentStudentRepository.existsByParentIdAndStudentId(user.getId(), student.getId())) {
            parentStudentRepository.save(ParentStudent.create(user, student));
        }

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 성별은 필수입니다");
        }
        String gender = request.studentGender().trim().toUpperCase();
        if (!"M".equals(gender) && !"F".equals(gender)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 성별은 M 또는 F로 입력해야 합니다");
        }
        if (isBlank(request.classCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학급 코드는 필수입니다");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
