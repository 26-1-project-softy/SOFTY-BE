package com.softy.be.repository.school;

import com.softy.be.domain.school.ParentStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {
    Optional<ParentStudent> findFirstByParentIdOrderByIdDesc(Long parentId);
    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);
}
