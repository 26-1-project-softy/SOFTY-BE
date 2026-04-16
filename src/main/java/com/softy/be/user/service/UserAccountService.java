package com.softy.be.user.service;

import com.softy.be.school.entity.ClassCode;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.entity.School;
import com.softy.be.school.entity.TeacherSetting;
import com.softy.be.school.repository.ClassCodeRepository;
import com.softy.be.school.repository.ClassroomRepository;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.SchoolRepository;
import com.softy.be.school.repository.TeacherSettingRepository;
import com.softy.be.school.service.ClassCodeService;
import com.softy.be.user.dto.TeacherClassUpdateRequest;
import com.softy.be.user.repository.SocialAccountRepository;
import com.softy.be.user.repository.UserRepository;
import com.softy.be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ClassroomRepository classroomRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final SchoolRepository schoolRepository;
    private final ClassCodeRepository classCodeRepository;
    private final TeacherSettingRepository teacherSettingRepository;
    private final ClassCodeService classCodeService;

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

        socialAccountRepository.deleteAllByUserId(userId);
        user.withdraw();
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

        Classroom classroom = classroomRepository.save(
                Classroom.create(request.grade(), request.classNumber(), school, user)
        );

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

        String classCode = classCodeRepository.findFirstByClassroomIdOrderByIdDesc(classroom.getId())
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

    private TeacherSettingScheduleResult toTeacherSettingScheduleResult(TeacherSetting setting) {
        return new TeacherSettingScheduleResult(
                setting.getDayOfWeek(),
                toLocalTime(setting.getStartTime()),
                toLocalTime(setting.getEndTime())
        );
    }

    private LocalTime toLocalTime(LocalDateTime value) {
        return value == null ? null : value.toLocalTime();
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
