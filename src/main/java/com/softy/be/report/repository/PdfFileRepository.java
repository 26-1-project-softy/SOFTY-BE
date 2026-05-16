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
                    JOIN users u ON u.id = pf.created_by
                    JOIN user_role ur ON ur.user_id = u.id
                    WHERE UPPER(ur.role) = 'TEACHER'
                    GROUP BY u.id, u.name
                    ORDER BY COUNT(pf.id) DESC, u.name ASC, u.id ASC
                    """,
            nativeQuery = true
    )
    List<TeacherPdfCountRow> findTeacherPdfCounts();
}
