package com.softy.be.chat.repository;

import com.softy.be.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
                        ) AS counterpartName,
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
                        COALESCE(
                            (
                                SELECT CASE
                                           WHEN m.modify_content IS NOT NULL AND BTRIM(m.modify_content) <> '' THEN BTRIM(m.modify_content)
                                           WHEN m.content IS NOT NULL THEN BTRIM(m.content)
                                           ELSE ''
                                       END
                                FROM message m
                                WHERE m.chat_room_id = cr.id
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS lastMessage,
                        COALESCE(
                            (
                                SELECT m.created_at
                                FROM message m
                                WHERE m.chat_room_id = cr.id
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT 1
                            ),
                            cr.created_at
                        ) AS lastMessageAt,
                        COALESCE(
                            (
                                SELECT crm4.unread_count
                                FROM chat_room_user_map crm4
                                WHERE crm4.chat_room_id = cr.id
                                  AND crm4.user_id = :teacherId
                                ORDER BY crm4.id DESC
                                LIMIT 1
                            ),
                            0
                        ) AS unreadCount,
                        cr.status AS status,
                        cr.intent_label AS intentLabel
                    FROM chat_room cr
                    WHERE EXISTS (
                        SELECT 1
                        FROM chat_room_user_map crm
                        WHERE crm.chat_room_id = cr.id
                          AND crm.user_id = :teacherId
                    )
                      AND (
                        :cursor IS NULL
                        OR (
                            COALESCE(
                                (
                                    SELECT m.created_at
                                    FROM message m
                                    WHERE m.chat_room_id = cr.id
                                    ORDER BY m.created_at DESC, m.id DESC
                                    LIMIT 1
                                ),
                                cr.created_at
                            ),
                            cr.id
                        ) < (
                            COALESCE(
                                (
                                    SELECT m2.created_at
                                    FROM message m2
                                    WHERE m2.chat_room_id = :cursor
                                    ORDER BY m2.created_at DESC, m2.id DESC
                                    LIMIT 1
                                ),
                                (
                                    SELECT cr2.created_at
                                    FROM chat_room cr2
                                    WHERE cr2.id = :cursor
                                )
                            ),
                            :cursor
                        )
                    )
                    ORDER BY
                        COALESCE(
                            (
                                SELECT m.created_at
                                FROM message m
                                WHERE m.chat_room_id = cr.id
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT 1
                            ),
                            cr.created_at
                        ) DESC,
                        cr.id DESC
                    """,
            nativeQuery = true
    )
    List<ChatRoomListRow> findChatRoomsByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        cr.id AS chatRoomId,
                        (
                            SELECT tu.name
                            FROM chat_room_user_map crm2
                            JOIN users tu ON tu.id = crm2.user_id
                            WHERE crm2.chat_room_id = cr.id
                              AND UPPER(tu.role) = 'TEACHER'
                            ORDER BY crm2.id DESC
                            LIMIT 1
                        ) AS counterpartName,
                        (
                            SELECT s.name
                            FROM parent_student ps
                            JOIN student s ON s.id = ps.student_id
                            WHERE ps.parent_id = :parentId
                            ORDER BY ps.id DESC
                            LIMIT 1
                        ) AS studentName,
                        COALESCE(
                            (
                                SELECT CASE
                                           WHEN m.modify_content IS NOT NULL AND BTRIM(m.modify_content) <> '' THEN BTRIM(m.modify_content)
                                           WHEN m.content IS NOT NULL THEN BTRIM(m.content)
                                           ELSE ''
                                       END
                                FROM message m
                                WHERE m.chat_room_id = cr.id
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS lastMessage,
                        COALESCE(
                            (
                                SELECT m.created_at
                                FROM message m
                                WHERE m.chat_room_id = cr.id
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT 1
                            ),
                            cr.created_at
                        ) AS lastMessageAt,
                        COALESCE(
                            (
                                SELECT crm4.unread_count
                                FROM chat_room_user_map crm4
                                WHERE crm4.chat_room_id = cr.id
                                  AND crm4.user_id = :parentId
                                ORDER BY crm4.id DESC
                                LIMIT 1
                            ),
                            0
                        ) AS unreadCount,
                        cr.status AS status,
                        cr.intent_label AS intentLabel
                    FROM chat_room cr
                    WHERE EXISTS (
                        SELECT 1
                        FROM chat_room_user_map crm
                        WHERE crm.chat_room_id = cr.id
                          AND crm.user_id = :parentId
                    )
                      AND (
                        :cursor IS NULL
                        OR (
                            COALESCE(
                                (
                                    SELECT m.created_at
                                    FROM message m
                                    WHERE m.chat_room_id = cr.id
                                    ORDER BY m.created_at DESC, m.id DESC
                                    LIMIT 1
                                ),
                                cr.created_at
                            ),
                            cr.id
                        ) < (
                            COALESCE(
                                (
                                    SELECT m2.created_at
                                    FROM message m2
                                    WHERE m2.chat_room_id = :cursor
                                    ORDER BY m2.created_at DESC, m2.id DESC
                                    LIMIT 1
                                ),
                                (
                                    SELECT cr2.created_at
                                    FROM chat_room cr2
                                    WHERE cr2.id = :cursor
                                )
                            ),
                            :cursor
                        )
                    )
                    ORDER BY
                        COALESCE(
                            (
                                SELECT m.created_at
                                FROM message m
                                WHERE m.chat_room_id = cr.id
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT 1
                            ),
                            cr.created_at
                        ) DESC,
                        cr.id DESC
                    """,
            nativeQuery = true
    )
    List<ChatRoomListRow> findChatRoomsByParentId(
            @Param("parentId") Long parentId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM chat_room_user_map crm
                        WHERE crm.chat_room_id = :chatRoomId
                          AND crm.user_id = :userId
                    )
                    """,
            nativeQuery = true
    )
    boolean existsParticipantByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

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
