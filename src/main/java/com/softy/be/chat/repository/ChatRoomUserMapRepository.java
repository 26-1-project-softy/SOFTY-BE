package com.softy.be.chat.repository;

import com.softy.be.chat.entity.ChatRoomUserMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomUserMapRepository extends JpaRepository<ChatRoomUserMap, Long> {
}
