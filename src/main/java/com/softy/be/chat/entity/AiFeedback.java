package com.softy.be.chat.entity;

import com.softy.be.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_feedback")
@Getter
@NoArgsConstructor
public class AiFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_analysis_id", nullable = false)
    private MessageAnalysis messageAnalysis;

    @Column(nullable = false)
    private String type;

    @Column(name = "actual_risk_score")
    private Integer actualRiskScore;

    public static AiFeedback create(MessageAnalysis messageAnalysis, String type, int actualRiskScore) {
        AiFeedback feedback = new AiFeedback();
        feedback.messageAnalysis = messageAnalysis;
        feedback.type = type;
        feedback.actualRiskScore = actualRiskScore;
        return feedback;
    }

    public void updateScore(int actualRiskScore) {
        this.actualRiskScore = actualRiskScore;
    }
}
