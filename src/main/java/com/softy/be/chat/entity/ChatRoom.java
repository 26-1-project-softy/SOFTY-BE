package com.softy.be.chat.entity;

import com.softy.be.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String intentLabel;

    @Enumerated(EnumType.STRING)
    private ChatRoomStatus status;

    public static ChatRoom create(String intentLabel, ChatRoomStatus status) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.intentLabel = intentLabel;
        chatRoom.status = status;
        return chatRoom;
    }
}
