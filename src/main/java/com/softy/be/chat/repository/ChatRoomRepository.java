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
                        COALESCE(
                            (
                                SELECT pu.name
                                FROM chat_room_user_map crm2
                                JOIN users pu ON pu.id = crm2.user_id
                                WHERE crm2.chat_room_id = cr.id
                                  AND crm2.participant_role = 'PARENT'
                                ORDER BY crm2.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS counterpartName,
                        COALESCE(
                            (
                                SELECT s.name
                                FROM parent_student ps
                                JOIN student s ON s.id = ps.student_id
                                WHERE ps.parent_id = (
                                    SELECT crm3.user_id
                                    FROM chat_room_user_map crm3
                                    WHERE crm3.chat_room_id = cr.id
                                      AND crm3.participant_role = 'PARENT'
                                    ORDER BY crm3.id DESC
                                    LIMIT 1
                                )
                                ORDER BY ps.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS studentName,
                        cr.intent_label AS intentLabel,
                        cr.status AS status
                    FROM chat_room cr
                    JOIN chat_room_user_map my_map
                      ON my_map.chat_room_id = cr.id
                     AND my_map.user_id = :teacherId
                    WHERE cr.id = :chatRoomId
                    """,
            nativeQuery = true
    )
    ChatRoomDetailRow findChatRoomDetailByTeacherIdAndChatRoomId(
            @Param("teacherId") Long teacherId,
            @Param("chatRoomId") Long chatRoomId
    );

    @Query(
            value = """
                    SELECT
                        cr.id AS chatRoomId,
                        COALESCE(
                            (
                                SELECT tu.name
                                FROM chat_room_user_map crm2
                                JOIN users tu ON tu.id = crm2.user_id
                                WHERE crm2.chat_room_id = cr.id
                                  AND crm2.participant_role = 'TEACHER'
                                ORDER BY crm2.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS counterpartName,
                        COALESCE(
                            (
                                SELECT s.name
                                FROM parent_student ps
                                JOIN student s ON s.id = ps.student_id
                                WHERE ps.parent_id = :parentId
                                ORDER BY ps.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS studentName,
                        cr.intent_label AS intentLabel,
                        cr.status AS status
                    FROM chat_room cr
                    JOIN chat_room_user_map my_map
                      ON my_map.chat_room_id = cr.id
                     AND my_map.user_id = :parentId
                    WHERE cr.id = :chatRoomId
                    """,
            nativeQuery = true
    )
    ChatRoomDetailRow findChatRoomDetailByParentIdAndChatRoomId(
            @Param("parentId") Long parentId,
            @Param("chatRoomId") Long chatRoomId
    );

    @Query(
            value = """
                    WITH latest_message AS (
                        SELECT
                            x.chat_room_id,
                            x.last_message,
                            x.created_at AS last_message_at
                        FROM (
                            SELECT
                                m.chat_room_id,
                                CASE
                                    WHEN m.modify_content IS NOT NULL THEN m.modify_content
                                    WHEN m.content IS NOT NULL THEN m.content
                                    ELSE ''
                                END AS last_message,
                                m.created_at,
                                ROW_NUMBER() OVER (
                                    PARTITION BY m.chat_room_id
                                    ORDER BY m.created_at DESC, m.id DESC
                                ) AS rn
                            FROM message m
                        ) x
                        WHERE x.rn = 1
                    ),
                    cursor_info AS (
                        SELECT
                            cr.id AS chat_room_id,
                            COALESCE(lm.last_message_at, cr.created_at) AS sort_at
                        FROM chat_room cr
                        LEFT JOIN latest_message lm ON lm.chat_room_id = cr.id
                        WHERE cr.id = :cursor
                    )
                    SELECT
                        cr.id AS chatRoomId,
                        COALESCE(
                            (
                                SELECT pu.name
                                FROM chat_room_user_map crm2
                                JOIN users pu ON pu.id = crm2.user_id
                                WHERE crm2.chat_room_id = cr.id
                                  AND crm2.participant_role = 'PARENT'
                                ORDER BY crm2.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS counterpartName,
                        COALESCE(
                            (
                                SELECT s.name
                                FROM parent_student ps
                                JOIN student s ON s.id = ps.student_id
                                WHERE ps.parent_id = (
                                    SELECT crm3.user_id
                                    FROM chat_room_user_map crm3
                                    WHERE crm3.chat_room_id = cr.id
                                      AND crm3.participant_role = 'PARENT'
                                    ORDER BY crm3.id DESC
                                    LIMIT 1
                                )
                                ORDER BY ps.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS studentName,
                        COALESCE(lm.last_message, '') AS lastMessage,
                        COALESCE(lm.last_message_at, cr.created_at) AS lastMessageAt,
                        COALESCE(my_map.unread_count, 0) AS unreadCount,
                        cr.status AS status,
                        cr.intent_label AS intentLabel
                    FROM chat_room cr
                    JOIN chat_room_user_map my_map
                      ON my_map.chat_room_id = cr.id
                     AND my_map.user_id = :teacherId
                    LEFT JOIN latest_message lm ON lm.chat_room_id = cr.id
                    WHERE :cursor IS NULL
                       OR (
                            COALESCE(lm.last_message_at, cr.created_at),
                            cr.id
                       ) < (
                            SELECT ci.sort_at, ci.chat_room_id
                            FROM cursor_info ci
                       )
                    ORDER BY
                        COALESCE(lm.last_message_at, cr.created_at) DESC,
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
                    WITH latest_message AS (
                        SELECT
                            x.chat_room_id,
                            x.last_message,
                            x.created_at AS last_message_at
                        FROM (
                            SELECT
                                m.chat_room_id,
                                CASE
                                    WHEN m.modify_content IS NOT NULL THEN m.modify_content
                                    WHEN m.content IS NOT NULL THEN m.content
                                    ELSE ''
                                END AS last_message,
                                m.created_at,
                                ROW_NUMBER() OVER (
                                    PARTITION BY m.chat_room_id
                                    ORDER BY m.created_at DESC, m.id DESC
                                ) AS rn
                            FROM message m
                        ) x
                        WHERE x.rn = 1
                    ),
                    cursor_info AS (
                        SELECT
                            cr.id AS chat_room_id,
                            COALESCE(lm.last_message_at, cr.created_at) AS sort_at
                        FROM chat_room cr
                        LEFT JOIN latest_message lm ON lm.chat_room_id = cr.id
                        WHERE cr.id = :cursor
                    )
                    SELECT
                        cr.id AS chatRoomId,
                        COALESCE(
                            (
                                SELECT tu.name
                                FROM chat_room_user_map crm2
                                JOIN users tu ON tu.id = crm2.user_id
                                WHERE crm2.chat_room_id = cr.id
                                  AND crm2.participant_role = 'TEACHER'
                                ORDER BY crm2.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS counterpartName,
                        COALESCE(
                            (
                                SELECT s.name
                                FROM parent_student ps
                                JOIN student s ON s.id = ps.student_id
                                WHERE ps.parent_id = :parentId
                                ORDER BY ps.id DESC
                                LIMIT 1
                            ),
                            ''
                        ) AS studentName,
                        COALESCE(lm.last_message, '') AS lastMessage,
                        COALESCE(lm.last_message_at, cr.created_at) AS lastMessageAt,
                        COALESCE(my_map.unread_count, 0) AS unreadCount,
                        cr.status AS status,
                        cr.intent_label AS intentLabel
                    FROM chat_room cr
                    JOIN chat_room_user_map my_map
                      ON my_map.chat_room_id = cr.id
                     AND my_map.user_id = :parentId
                    LEFT JOIN latest_message lm ON lm.chat_room_id = cr.id
                    WHERE :cursor IS NULL
                       OR (
                            COALESCE(lm.last_message_at, cr.created_at),
                            cr.id
                       ) < (
                            SELECT ci.sort_at, ci.chat_room_id
                            FROM cursor_info ci
                       )
                    ORDER BY
                        COALESCE(lm.last_message_at, cr.created_at) DESC,
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
                              AND crm2.participant_role = 'PARENT'
                            ORDER BY crm2.id DESC
                            LIMIT 1
                        ) AS parentName,
                        (
                            SELECT s.name
                            FROM parent_student ps
                            JOIN student s ON s.id = ps.student_id
                            WHERE ps.parent_id = (
                                SELECT crm3.user_id
                                FROM chat_room_user_map crm3
                                WHERE crm3.chat_room_id = cr.id
                                  AND crm3.participant_role = 'PARENT'
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
                      AND crm.participant_role = 'PARENT'
                    ORDER BY crm.id DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    String findParentNameByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
