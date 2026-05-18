package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long>, AiFeedbackRepositoryCustom {

    Optional<AiFeedback> findFirstByMessageAnalysisId(Long messageAnalysisId);
}
