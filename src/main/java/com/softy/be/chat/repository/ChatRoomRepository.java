package com.softy.be.chat.repository;

import com.softy.be.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query(
            value = """
                    SELECT
                        cr.id AS chatRoomId,
                        (
                            SELECT pu.name
                            FROM chat_room_user_map crm2
                            JOIN users pu ON pu.id = crm2.user_id
                            WHERE crm2.chat_room_id = cr.id
                              AND UPPER(pu.role) = 'PARENT'
                            ORDER BY crm2.id DESC
                            LIMIT 1
                        ) AS parentName,
                        (
                            SELECT s.name
                            FROM parent_student ps
                            JOIN student s ON s.id = ps.student_id
                            WHERE ps.parent_id = (
                                SELECT pu2.id
                                FROM chat_room_user_map crm3
                                JOIN users pu2 ON pu2.id = crm3.user_id
                                WHERE crm3.chat_room_id = cr.id
                                  AND UPPER(pu2.role) = 'PARENT'
                                ORDER BY crm3.id DESC
                                LIMIT 1
                            )
                            ORDER BY ps.id DESC
                            LIMIT 1
                        ) AS studentName,
                        cr.intent_label AS intentLabel,
                        cr.status AS status,
                        COALESCE(
                            (SELECT MAX(m.created_at) FROM message m WHERE m.chat_room_id = cr.id),
                            cr.created_at
                        ) AS lastMessageAt
                    FROM chat_room cr
                    WHERE EXISTS (
                        SELECT 1
                        FROM chat_room_user_map crm
                        WHERE crm.chat_room_id = cr.id
                          AND crm.user_id = :teacherId
                    )
                    ORDER BY
                        COALESCE(
                            (SELECT MAX(m.created_at) FROM message m WHERE m.chat_room_id = cr.id),
                            cr.created_at
                        ) DESC,
                        cr.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM chat_room cr
                    WHERE EXISTS (
                        SELECT 1
                        FROM chat_room_user_map crm
                        WHERE crm.chat_room_id = cr.id
                          AND crm.user_id = :teacherId
                    )
                    """,
            nativeQuery = true
    )
    Page<ReportChatRoomRow> findReportChatRoomsByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);

    @Query(
            value = """
                    SELECT pu.name
                    FROM chat_room_user_map crm
                    JOIN users pu ON pu.id = crm.user_id
                    WHERE crm.chat_room_id = :chatRoomId
                      AND UPPER(pu.role) = 'PARENT'
                    ORDER BY crm.id DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    String findParentNameByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
