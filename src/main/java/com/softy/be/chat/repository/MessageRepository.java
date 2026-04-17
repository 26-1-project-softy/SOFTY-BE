package com.softy.be.chat.repository;

import com.softy.be.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m
            FROM Message m
            JOIN FETCH m.sender
            WHERE m.chatRoom.id = :chatRoomId
            ORDER BY m.createdAt ASC, m.id ASC
            """)
    List<Message> findAllByChatRoomIdForReport(@Param("chatRoomId") Long chatRoomId);
}

