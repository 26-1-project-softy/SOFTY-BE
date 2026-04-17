package com.softy.be.report.service;

import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.chat.repository.ReportChatRoomRow;
import com.softy.be.report.dto.ReportChatRoomItemData;
import com.softy.be.report.dto.ReportChatRoomListData;
import com.softy.be.report.dto.ReportPdfCreateData;
import com.softy.be.report.entity.PdfFile;
import com.softy.be.report.repository.PdfFileRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String ROLE_TEACHER = "TEACHER";
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final PdfFileRepository pdfFileRepository;
    private final ReportPdfRenderService reportPdfRenderService;
    private final ReportS3StorageService reportS3StorageService;

    @Transactional(readOnly = true)
    public ReportChatRoomListData getChatRoomsForReport(Long userId, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0 이상이어야 합니다");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~100 사이여야 합니다");
        }

        getTeacherOrThrow(userId);

        Page<ReportChatRoomRow> result = chatRoomRepository.findReportChatRoomsByTeacherId(
                userId,
                PageRequest.of(page, size)
        );

        return new ReportChatRoomListData(
                result.getContent().stream()
                        .map(row -> new ReportChatRoomItemData(
                                row.getChatRoomId(),
                                nullToEmpty(row.getParentName()),
                                nullToEmpty(row.getStudentName()),
                                row.getIntentLabel(),
                                nullToEmpty(row.getStatus()),
                                row.getLastMessageAt()
                        ))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    private User getTeacherOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if (!ROLE_TEACHER.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 계정만 증빙 리포트를 조회할 수 있습니다");
        }
        return user;
    }

    @Transactional
    public ReportPdfCreateData createPdf(Long userId, Long chatRoomId) {
        getTeacherOrThrow(userId);
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"));

        List<Message> messages = messageRepository.findAllByChatRoomIdForReport(chatRoomId);
        byte[] pdfBytes = reportPdfRenderService.render(chatRoomId, chatRoom.getIntentLabel(), messages);

        String parentName = safeParentName(chatRoomRepository.findParentNameByChatRoomId(chatRoomId));
        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        String displayFileName = "증빙리포트_" + parentName + "_" + timestamp + ".pdf";
        String objectFileName = "report-" + UUID.randomUUID() + ".pdf";
        String objectKey = reportS3StorageService.buildObjectKey(chatRoomId, objectFileName);
        String s3Uri = reportS3StorageService.uploadPdf(objectKey, pdfBytes);

        PdfFile pdfFile = pdfFileRepository.save(PdfFile.create(chatRoom, s3Uri, displayFileName));
        String downloadUrl = reportS3StorageService.createDownloadUrl(objectKey);

        return new ReportPdfCreateData(
                pdfFile.getId(),
                pdfFile.getFileName(),
                downloadUrl,
                reportS3StorageService.getPresignedExpireSeconds()
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String safeParentName(String parentName) {
        String fallback = "학부모";
        if (parentName == null || parentName.isBlank()) {
            return fallback;
        }
        String sanitized = parentName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized.isBlank() ? fallback : sanitized;
    }
}
