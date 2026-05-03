package com.softy.be.chat.entity;

import com.softy.be.common.entity.BaseEntity;
import com.softy.be.user.entity.User;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "message_analysis")
@Getter
@NoArgsConstructor
public class MessageAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "original_content", columnDefinition = "TEXT", nullable = false)
    private String originalContent;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "recommended_message", columnDefinition = "TEXT")
    private String recommendedMessage;

    @Column(name = "is_recommendation_adopted", nullable = false)
    private Boolean recommendationAdopted;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public static MessageAnalysis create(
            ChatRoom chatRoom,
            User teacher,
            String originalContent,
            String riskLevel,
            String recommendedMessage,
            LocalDateTime expiresAt
    ) {
        MessageAnalysis analysis = new MessageAnalysis();
        analysis.chatRoom = chatRoom;
        analysis.teacher = teacher;
        analysis.originalContent = originalContent;
        analysis.riskLevel = riskLevel;
        analysis.recommendedMessage = recommendedMessage;
        analysis.recommendationAdopted = false;
        analysis.expiresAt = expiresAt;
        return analysis;
    }

    public void markRecommendationAdopted() {
        this.recommendationAdopted = true;
    }

}
