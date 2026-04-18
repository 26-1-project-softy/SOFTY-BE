package com.softy.be.report.repository;

import com.softy.be.report.entity.PdfFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PdfFileRepository extends JpaRepository<PdfFile, Long> {

    @Query(
            value = """
                    SELECT
                        u.id AS teacherId,
                        u.name AS teacherName,
                        COUNT(pf.id) AS pdfCount
                    FROM pdf_file pf
                    JOIN (
                        SELECT DISTINCT chat_room_id, user_id
                        FROM chat_room_user_map
                    ) crm ON crm.chat_room_id = pf.chat_room_id
                    JOIN users u ON u.id = crm.user_id
                    WHERE UPPER(u.role) = 'TEACHER'
                    GROUP BY u.id, u.name
                    ORDER BY COUNT(pf.id) DESC, u.name ASC, u.id ASC
                    """,
            nativeQuery = true
    )
    List<TeacherPdfCountRow> findTeacherPdfCounts();
}

