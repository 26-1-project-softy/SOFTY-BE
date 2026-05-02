package com.softy.be.user.service;

import com.softy.be.auth.service.KakaoOAuthClient;
import com.softy.be.school.entity.ClassCode;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.entity.School;
import com.softy.be.school.entity.Student;
import com.softy.be.school.entity.TeacherSetting;
import com.softy.be.school.repository.ClassCodeRepository;
import com.softy.be.school.repository.ClassroomRepository;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.SchoolRepository;
import com.softy.be.school.repository.StudentRepository;
import com.softy.be.school.repository.TeacherSettingRepository;
import com.softy.be.school.service.ClassCodeService;
import com.softy.be.user.dto.TeacherClassUpdateRequest;
import com.softy.be.user.dto.ParentClassPreviewRequest;
import com.softy.be.user.dto.ParentClassUpdateRequest;
import com.softy.be.user.dto.TeacherWorkHoursScheduleRequest;
import com.softy.be.user.dto.TeacherWorkHoursUpdateRequest;
import com.softy.be.user.repository.SocialAccountRepository;
import com.softy.be.user.repository.UserRepository;
import com.softy.be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private static final String KAKAO_PROVIDER = "KAKAO";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ClassroomRepository classroomRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final SchoolRepository schoolRepository;
    private final ClassCodeRepository classCodeRepository;
    private final StudentRepository studentRepository;
    private final TeacherSettingRepository teacherSettingRepository;
    private final ClassCodeService classCodeService;
    private final KakaoOAuthClient kakaoOAuthClient;

    @Transactional(readOnly = true)
    public UserMeResult getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        Integer grade = null;
        Integer classNumber = null;

        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(userId).orElse(null);
            if (classroom != null) {
                grade = classroom.getGrade();
                classNumber = classroom.getClassNumber();
            }
        } else if ("PARENT".equalsIgnoreCase(user.getRole())) {
            ParentStudent mapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(userId).orElse(null);
            if (mapping != null && mapping.getStudent() != null && mapping.getStudent().getClassroom() != null) {
                grade = mapping.getStudent().getClassroom().getGrade();
                classNumber = mapping.getStudent().getClassroom().getClassNumber();
            }
        }

        return new UserMeResult(user.getRole(), user.getName(), grade, classNumber);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if ("WITHDRAWN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 탈퇴한 계정입니다");
        }

        socialAccountRepository.findFirstByUserIdAndProviderOrderByIdDesc(userId, KAKAO_PROVIDER)
                .ifPresent(socialAccount -> unlinkKakaoOrThrow(socialAccount.getProviderUserId()));

        socialAccountRepository.deleteAllByUserId(userId);
        user.withdraw();
    }

    private void unlinkKakaoOrThrow(String providerUserId) {
        try {
            kakaoOAuthClient.unlinkUserByAdminKey(providerUserId);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "카카오 연결 해제에 실패하여 회원 탈퇴를 중단했습니다.",
                    e
            );
        }
    }

    @Transactional
    public TeacherClassUpdateResult updateTeacherClass(Long userId, TeacherClassUpdateRequest request) {
        validateTeacherClassUpdateRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 계정만 학급을 변경할 수 있습니다");
        }

        String schoolName = request.schoolName().trim();
        School school = schoolRepository.findByName(schoolName)
                .orElseGet(() -> schoolRepository.save(School.create(schoolName)));

        Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(userId)
                .map(existing -> {
                    existing.updateClassInfo(request.grade(), request.classNumber(), school);
                    return existing;
                })
                .orElseGet(() -> classroomRepository.save(
                        Classroom.create(request.grade(), request.classNumber(), school, user)
                ));

        String classCode = classCodeService.createClassCodeForClassroom(classroom);
        return new TeacherClassUpdateResult(classCode);
    }

    @Transactional(readOnly = true)
    public TeacherSettingResult getTeacherSetting(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 계정만 설정 정보를 조회할 수 있습니다");
        }

        Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "교사 학급 정보를 찾을 수 없습니다"));

        String classCode = classCodeRepository.findFirstByClassroomIdAndIsActiveTrueOrderByIdDesc(classroom.getId())
                .map(ClassCode::getCode)
                .orElse(null);

        List<TeacherSettingScheduleResult> schedules = teacherSettingRepository
                .findByTeacherIdOrderByDayOfWeekAscIdAsc(userId)
                .stream()
                .map(this::toTeacherSettingScheduleResult)
                .toList();

        String schoolName = classroom.getSchool() == null ? null : classroom.getSchool().getName();

        return new TeacherSettingResult(
                classroom.getGrade(),
                classroom.getClassNumber(),
                schoolName,
                classCode,
                user.getName(),
                schedules
        );
    }

    @Transactional(readOnly = true)
    public ParentSettingResult getParentSetting(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!"PARENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 계정만 설정 정보를 조회할 수 있습니다.");
        }

        ParentStudent mapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "연결된 자녀 정보를 찾을 수 없습니다."));

        if (mapping.getStudent() == null || mapping.getStudent().getClassroom() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학생의 학급 정보를 찾을 수 없습니다.");
        }

        Classroom classroom = mapping.getStudent().getClassroom();
        User teacher = classroom.getTeacher();
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "담당 교사 정보를 찾을 수 없습니다.");
        }

        List<ParentSettingScheduleResult> schedules = teacherSettingRepository
                .findByTeacherIdOrderByDayOfWeekAscIdAsc(teacher.getId())
                .stream()
                .map(this::toParentSettingScheduleResult)
                .toList();

        return new ParentSettingResult(
                classroom.getGrade(),
                classroom.getClassNumber(),
                mapping.getStudent().getName(),
                teacher.getName(),
                schedules
        );
    }

    @Transactional(readOnly = true)
    public ParentClassPreviewResult previewParentClassChange(Long userId, ParentClassPreviewRequest request) {
        validateParentClassPreviewRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!"PARENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 계정만 학급 변경을 요청할 수 있습니다.");
        }

        ClassCode classCode = classCodeRepository.findFirstByCodeAndIsActiveTrueOrderByIdDesc(request.classCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효한 학급 코드를 찾을 수 없습니다."));

        Classroom classroom = classCode.getClassroom();
        if (classroom == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학급 정보를 찾을 수 없습니다.");
        }

        String schoolName = classroom.getSchool() == null ? null : classroom.getSchool().getName();
        return new ParentClassPreviewResult(
                classCode.getCode(),
                schoolName,
                classroom.getGrade(),
                classroom.getClassNumber()
        );
    }

    @Transactional
    public ParentClassUpdateResult updateParentClass(Long userId, ParentClassUpdateRequest request) {
        validateParentClassUpdateRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!"PARENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 계정만 학급 변경을 요청할 수 있습니다.");
        }

        ParentStudent mapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "연결된 자녀 정보를 찾을 수 없습니다."));

        Student currentStudent = mapping.getStudent();
        if (currentStudent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "연결된 학생 정보를 찾을 수 없습니다.");
        }

        ClassCode classCode = classCodeRepository.findFirstByCodeAndIsActiveTrueOrderByIdDesc(request.classCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효한 학급 코드를 찾을 수 없습니다."));

        Classroom classroom = classCode.getClassroom();
        if (classroom == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학급 정보를 찾을 수 없습니다.");
        }

        Student targetStudent = studentRepository
                .findFirstByClassroomIdAndNameAndBirthdayAndGenderOrderByIdDesc(
                        classroom.getId(),
                        currentStudent.getName(),
                        currentStudent.getBirthday(),
                        currentStudent.getGender()
                )
                .orElseGet(() -> studentRepository.save(
                        Student.create(
                                currentStudent.getName(),
                                currentStudent.getBirthday(),
                                currentStudent.getGender(),
                                classroom
                        )
                ));

        mapping.changeStudent(targetStudent);

        String schoolName = classroom.getSchool() == null ? null : classroom.getSchool().getName();
        return new ParentClassUpdateResult(
                schoolName,
                classroom.getGrade(),
                classroom.getClassNumber()
        );
    }

    @Transactional
    public void updateTeacherWorkHours(Long userId, TeacherWorkHoursUpdateRequest request) {
        if (request == null || request.schedules() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 계정만 근무시간을 변경할 수 있습니다");
        }

        List<TeacherSetting> newSettings = buildTeacherSettings(user, request.schedules());

        teacherSettingRepository.deleteAllByTeacherId(userId);
        if (!newSettings.isEmpty()) {
            teacherSettingRepository.saveAll(newSettings);
        }
    }

    private List<TeacherSetting> buildTeacherSettings(User user, List<TeacherWorkHoursScheduleRequest> schedules) {
        Set<Short> days = new HashSet<>();
        List<TeacherSetting> settings = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(1970, 1, 1);

        for (TeacherWorkHoursScheduleRequest schedule : schedules) {
            if (schedule == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "근무시간 정보가 비어 있습니다");
            }
            if (schedule.dayOfWeek() == null || schedule.dayOfWeek() < 1 || schedule.dayOfWeek() > 7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek는 1~7 사이여야 합니다");
            }
            if (!days.add(schedule.dayOfWeek())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요일은 중복될 수 없습니다");
            }
            if (schedule.startTime() == null || schedule.endTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "근무 시작/종료 시간은 필수입니다");
            }
            if (!schedule.startTime().isBefore(schedule.endTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "근무 시작 시간은 종료 시간보다 빨라야 합니다");
            }

            settings.add(TeacherSetting.create(
                    user,
                    schedule.dayOfWeek(),
                    LocalDateTime.of(baseDate, schedule.startTime()),
                    LocalDateTime.of(baseDate, schedule.endTime())
            ));
        }

        return settings;
    }

    private TeacherSettingScheduleResult toTeacherSettingScheduleResult(TeacherSetting setting) {
        return new TeacherSettingScheduleResult(
                setting.getDayOfWeek(),
                toLocalTime(setting.getStartTime()),
                toLocalTime(setting.getEndTime())
        );
    }

    private ParentSettingScheduleResult toParentSettingScheduleResult(TeacherSetting setting) {
        return new ParentSettingScheduleResult(
                setting.getDayOfWeek(),
                toLocalTime(setting.getStartTime()),
                toLocalTime(setting.getEndTime())
        );
    }

    private LocalTime toLocalTime(LocalDateTime value) {
        return value == null ? null : value.toLocalTime();
    }

    private void validateParentClassPreviewRequest(ParentClassPreviewRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (isBlank(request.classCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "classCode는 필수입니다.");
        }
    }

    private void validateParentClassUpdateRequest(ParentClassUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (isBlank(request.classCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "classCode는 필수입니다.");
        }
    }

    private void validateTeacherClassUpdateRequest(TeacherClassUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다");
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
