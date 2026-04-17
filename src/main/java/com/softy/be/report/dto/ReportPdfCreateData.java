package com.softy.be.report.dto;

public record ReportPdfCreateData(
        Long pdfId,
        String fileName,
        String downloadUrl,
        long expiresInSeconds
) {
}

