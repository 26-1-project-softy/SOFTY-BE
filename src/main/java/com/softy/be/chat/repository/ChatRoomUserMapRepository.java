package com.softy.be.chat.repository;

import com.softy.be.chat.entity.ChatRoomUserMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomUserMapRepository extends JpaRepository<ChatRoomUserMap, Long> {
    Optional<ChatRoomUserMap> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
    List<ChatRoomUserMap> findAllByChatRoomId(Long chatRoomId);
}
