package com.softy.be.admin.dto;

public record AdminTeacherPdfCountData(
        long teacherId,
        String teacherName,
        long pdfCount
) {
}
