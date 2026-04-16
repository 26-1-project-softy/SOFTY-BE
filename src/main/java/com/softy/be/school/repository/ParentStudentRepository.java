package com.softy.be.school.repository;

import com.softy.be.school.entity.ParentStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {
    Optional<ParentStudent> findFirstByParentIdOrderByIdDesc(Long parentId);
    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);
}
