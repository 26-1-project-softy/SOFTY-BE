package com.softy.be.repository.school;

import com.softy.be.domain.school.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findFirstByClassroomIdAndNameAndBirthdayAndGenderOrderByIdDesc(
            Long classroomId,
            String name,
            LocalDate birthday,
            String gender
    );
}
