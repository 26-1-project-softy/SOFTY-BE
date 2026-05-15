package com.softy.be.user.service;

import com.softy.be.auth.dto.TeacherSignupRequest;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.repository.ClassCodeRepository;
import com.softy.be.school.repository.ClassroomRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserRegistrationServiceTest {

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private ClassCodeRepository classCodeRepository;

    @Test
    void teacherSignupUpdatesRoleWithoutCreatingClassCode() {
        User savedUser = userRepository.save(User.createForKakao("kakao_nickname"));

        TeacherSignupResult signupResult = userRegistrationService.signupTeacher(
                savedUser.getId(),
                new TeacherSignupRequest("teacher_name", "softy_school", 3, 2)
        );

        User reloadedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(savedUser.getId()).orElseThrow();

        assertThat(signupResult.role()).isEqualTo("TEACHER");
        assertThat(reloadedUser.getRole()).isEqualTo("TEACHER");
        assertThat(reloadedUser.getName()).isEqualTo("teacher_name");
        assertThat(classCodeRepository.findFirstByClassroomIdAndIsActiveTrueOrderByIdDesc(classroom.getId())).isEmpty();
    }

    @Test
    void createTeacherClassCodeCreatesCodeAfterSignup() {
        User savedUser = userRepository.save(User.createForKakao("kakao_nickname"));

        userRegistrationService.signupTeacher(
                savedUser.getId(),
                new TeacherSignupRequest("teacher_name", "softy_school", 3, 2)
        );

        Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(savedUser.getId()).orElseThrow();
        ClassCodeCreateResult classCodeResult = userRegistrationService.createTeacherClassCode(savedUser.getId(), "TEACHER");

        assertThat(classCodeResult.classCode()).isNotBlank();
        assertThat(classCodeRepository.findFirstByClassroomIdAndIsActiveTrueOrderByIdDesc(classroom.getId()))
                .isPresent();
    }
}
