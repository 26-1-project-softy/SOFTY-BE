package com.softy.be.chat.repository;

import com.softy.be.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT m
            FROM Message m
            JOIN FETCH m.sender s
            WHERE m.chatRoom.id = :chatRoomId
              AND (:cursor IS NULL OR m.id < :cursor)
            ORDER BY m.id DESC
            """)
    List<Message> findPreviewMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM Message m
            WHERE m.chatRoom.id = :chatRoomId
              AND m.id < :cursor
            """)
    boolean existsOlderMessage(@Param("chatRoomId") Long chatRoomId, @Param("cursor") Long cursor);

    @Query("""
            SELECT COUNT(DISTINCT m.id)
            FROM Message m
            JOIN m.sender s
            JOIN s.userRoles ur,
                 ChatRoomUserMap crm
            WHERE crm.chatRoom = m.chatRoom
              AND crm.user = s
              AND UPPER(ur.role) = 'TEACHER'
              AND UPPER(crm.participantRole) = 'TEACHER'
            """)
    long countTeacherMessages();

    @Query("""
            SELECT COUNT(DISTINCT m.id)
            FROM Message m
            JOIN m.sender s
            JOIN s.userRoles ur,
                 ChatRoomUserMap crm
            WHERE crm.chatRoom = m.chatRoom
              AND crm.user = s
              AND UPPER(ur.role) = 'TEACHER'
              AND UPPER(crm.participantRole) = 'TEACHER'
              AND m.isDisputeRisk = true
            """)
    long countTeacherDisputeRiskMessages();

}

