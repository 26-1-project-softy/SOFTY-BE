package com.softy.be.school.repository;

import com.softy.be.school.entity.ClassCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassCodeRepository extends JpaRepository<ClassCode, Long> {
    boolean existsByCode(String code);
    Optional<ClassCode> findFirstByCodeAndIsActiveTrueOrderByIdDesc(String code);
    Optional<ClassCode> findFirstByClassroomIdAndIsActiveTrueOrderByIdDesc(Long classroomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ClassCode cc SET cc.isActive = false WHERE cc.classroom.id = :classroomId AND cc.isActive = true")
    int deactivateActiveCodesByClassroomId(@Param("classroomId") Long classroomId);
}
