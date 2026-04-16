package com.softy.be.school.repository;

import com.softy.be.school.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findFirstByTeacherIdOrderByIdDesc(Long teacherId);
}
