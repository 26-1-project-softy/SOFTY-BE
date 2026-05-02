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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "modify_content", columnDefinition = "TEXT")
    private String modifyContent;

    private Float similarityOriginal;
    private Float similarityModified;

    private Boolean isDisputeRisk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    public static Message create(String type, String content, ChatRoom chatRoom, User sender) {
        Message message = new Message();
        message.type = type;
        message.content = content;
        message.chatRoom = chatRoom;
        message.sender = sender;
        return message;
    }

    public String resolveReportContent() {
        if (hasText(modifyContent)) {
            return modifyContent.trim();
        }
        if (hasText(content)) {
            return content.trim();
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
