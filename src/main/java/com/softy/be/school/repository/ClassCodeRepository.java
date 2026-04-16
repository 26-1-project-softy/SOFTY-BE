package com.softy.be.school.repository;

import com.softy.be.school.entity.ClassCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassCodeRepository extends JpaRepository<ClassCode, Long> {
    boolean existsByCode(String code);
    Optional<ClassCode> findFirstByCodeOrderByIdDesc(String code);
    Optional<ClassCode> findFirstByClassroomIdOrderByIdDesc(Long classroomId);
}
