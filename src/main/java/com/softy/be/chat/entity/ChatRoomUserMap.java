package com.softy.be.chat.entity;

import com.softy.be.common.entity.BaseEntity;
import com.softy.be.user.entity.User;
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
@Table(name = "chat_room_user_map")
@Getter
@NoArgsConstructor
public class ChatRoomUserMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int unreadCount;

    private LocalDateTime lastReadAt;

    public static ChatRoomUserMap create(ChatRoom chatRoom, User user, int unreadCount, LocalDateTime lastReadAt) {
        ChatRoomUserMap mapping = new ChatRoomUserMap();
        mapping.chatRoom = chatRoom;
        mapping.user = user;
        mapping.unreadCount = unreadCount;
        mapping.lastReadAt = lastReadAt;
        return mapping;
    }
}
