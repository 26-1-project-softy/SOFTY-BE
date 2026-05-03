package com.softy.be.chat.repository;

import com.softy.be.chat.entity.MessageAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAnalysisRepository extends JpaRepository<MessageAnalysis, Long> {
}
