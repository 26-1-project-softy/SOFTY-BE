package com.softy.be.user.service;

import com.softy.be.school.entity.ClassCode;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.entity.School;
import com.softy.be.school.entity.Student;
import com.softy.be.school.repository.ClassCodeRepository;
import com.softy.be.school.repository.ClassroomRepository;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.SchoolRepository;
import com.softy.be.school.repository.StudentRepository;
import com.softy.be.school.repository.TeacherSettingRepository;
import com.softy.be.user.dto.ParentClassUpdateRequest;
import com.softy.be.user.dto.TeacherWorkHoursScheduleRequest;
import com.softy.be.user.dto.TeacherWorkHoursUpdateRequest;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountServiceTest {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

    @Autowired
    private ClassCodeRepository classCodeRepository;

    @Autowired
    private TeacherSettingRepository teacherSettingRepository;

    @Test
    void updateParentClassCreatesNewStudentAndKeepsOriginalStudentInPlace() {
        School school = schoolRepository.save(School.create("softy_school"));
        User sourceTeacher = userRepository.save(createTeacher("source_teacher"));
        User targetTeacher = userRepository.save(createTeacher("target_teacher"));
        User parent = userRepository.save(createParent("parent_name"));

        Classroom sourceClassroom = classroomRepository.save(Classroom.create(3, 1, school, sourceTeacher));
        Classroom targetClassroom = classroomRepository.save(Classroom.create(4, 2, school, targetTeacher));
        Student originalStudent = studentRepository.save(Student.create(
                "student_name",
                LocalDate.of(2016, 3, 2),
                "M",
                sourceClassroom
        ));
        parentStudentRepository.save(ParentStudent.create(parent, originalStudent));
        classCodeRepository.save(ClassCode.create("NEW-123", targetClassroom));

        userAccountService.updateParentClass(parent.getId(), "PARENT", new ParentClassUpdateRequest("NEW-123"));

        ParentStudent reloadedMapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(parent.getId()).orElseThrow();
        Student reloadedOriginalStudent = studentRepository.findById(originalStudent.getId()).orElseThrow();
        Student mappedStudent = studentRepository.findById(reloadedMapping.getStudent().getId()).orElseThrow();

        assertThat(reloadedOriginalStudent.getClassroom().getId()).isEqualTo(sourceClassroom.getId());
        assertThat(mappedStudent.getClassroom().getId()).isEqualTo(targetClassroom.getId());
        assertThat(mappedStudent.getId()).isNotEqualTo(originalStudent.getId());
        assertThat(studentRepository.count()).isEqualTo(2);
    }

    @Test
    void updateParentClassReusesExistingStudentInTargetClassroom() {
        School school = schoolRepository.save(School.create("softy_school"));
        User sourceTeacher = userRepository.save(createTeacher("source_teacher"));
        User targetTeacher = userRepository.save(createTeacher("target_teacher"));
        User parent = userRepository.save(createParent("parent_name"));

        Classroom sourceClassroom = classroomRepository.save(Classroom.create(3, 1, school, sourceTeacher));
        Classroom targetClassroom = classroomRepository.save(Classroom.create(4, 2, school, targetTeacher));
        Student originalStudent = studentRepository.save(Student.create(
                "student_name",
                LocalDate.of(2016, 3, 2),
                "M",
                sourceClassroom
        ));
        Student targetStudent = studentRepository.save(Student.create(
                "student_name",
                LocalDate.of(2016, 3, 2),
                "M",
                targetClassroom
        ));
        parentStudentRepository.save(ParentStudent.create(parent, originalStudent));
        classCodeRepository.save(ClassCode.create("REUSE-1", targetClassroom));

        userAccountService.updateParentClass(parent.getId(), "PARENT", new ParentClassUpdateRequest("REUSE-1"));

        ParentStudent reloadedMapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(parent.getId()).orElseThrow();
        Student reloadedOriginalStudent = studentRepository.findById(originalStudent.getId()).orElseThrow();

        assertThat(reloadedMapping.getStudent().getId()).isEqualTo(targetStudent.getId());
        assertThat(reloadedOriginalStudent.getClassroom().getId()).isEqualTo(sourceClassroom.getId());
        assertThat(studentRepository.count()).isEqualTo(2);
    }

    @Test
    void updateTeacherWorkHoursStoresTimeOnlyValues() {
        User teacher = userRepository.save(createTeacher("teacher_name"));

        userAccountService.updateTeacherWorkHours(
                teacher.getId(),
                "TEACHER",
                new TeacherWorkHoursUpdateRequest(List.of(
                        new TeacherWorkHoursScheduleRequest((short) 1, LocalTime.of(9, 30), LocalTime.of(16, 0)),
                        new TeacherWorkHoursScheduleRequest((short) 3, LocalTime.of(10, 0), LocalTime.of(18, 0))
                ))
        );

        var settings = teacherSettingRepository.findByTeacherIdOrderByDayOfWeekAscIdAsc(teacher.getId());

        assertThat(settings).hasSize(2);
        assertThat(settings.get(0).getDayOfWeek()).isEqualTo((short) 1);
        assertThat(settings.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(settings.get(0).getEndTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(settings.get(1).getDayOfWeek()).isEqualTo((short) 3);
        assertThat(settings.get(1).getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(settings.get(1).getEndTime()).isEqualTo(LocalTime.of(18, 0));
    }

    private User createTeacher(String name) {
        User user = User.createForKakao(name);
        user.completeTeacherSignup(name);
        return user;
    }

    private User createParent(String name) {
        User user = User.createForKakao(name);
        user.completeParentSignup(name);
        return user;
    }
}
