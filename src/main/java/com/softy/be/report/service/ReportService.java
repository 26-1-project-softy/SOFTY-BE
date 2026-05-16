package com.softy.be.report.service;

import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.chat.repository.ReportChatRoomRow;
import com.softy.be.report.dto.ReportChatPreviewData;
import com.softy.be.report.dto.ReportChatPreviewMessageItemData;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String ROLE_TEACHER = "TEACHER";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PREVIEW_SIZE = 100;
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final PdfFileRepository pdfFileRepository;
    private final ReportPdfRenderService reportPdfRenderService;
    private final ReportS3StorageService reportS3StorageService;

    @Transactional(readOnly = true)
    public ReportChatRoomListData getChatRoomsForReport(Long userId, String activeRole, int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~100 사이여야 합니다.");
        }

        getTeacherOrThrow(userId, activeRole);

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

    @Transactional(readOnly = true)
    public ReportChatPreviewData getChatPreview(Long userId, String activeRole, Long chatRoomId, Long cursor, int size) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 채팅방 ID입니다.");
        }
        if (size < 1 || size > MAX_PREVIEW_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1 이상 100 이하여야 합니다.");
        }
        if (cursor != null && cursor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 커서입니다.");
        }

        getTeacherOrThrow(userId, activeRole);

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        boolean hasAccess = chatRoomRepository.existsParticipantByChatRoomIdAndUserIdAndParticipantRole(chatRoomId, userId, activeRole);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근 권한이 없습니다.");
        }

        Pageable pageable = PageRequest.of(0, size);
        List<Message> descendingMessages = messageRepository.findPreviewMessages(chatRoomId, cursor, pageable);

        List<ReportChatPreviewMessageItemData> messages = new ArrayList<>(descendingMessages.size());
        for (Message message : descendingMessages) {
            messages.add(new ReportChatPreviewMessageItemData(
                    message.getId(),
                    message.getSender().getId().equals(userId),
                    message.resolveReportContent(),
                    message.getCreatedAt()
            ));
        }
        Collections.reverse(messages);

        Long nextCursor = messages.isEmpty() ? null : messages.get(0).messageId();
        boolean hasNext = nextCursor != null && messageRepository.existsOlderMessage(chatRoomId, nextCursor);

        return new ReportChatPreviewData(
                chatRoomId,
                messages,
                nextCursor,
                hasNext
        );
    }

    @Transactional
    public ReportPdfCreateData createPdf(Long userId, String activeRole, Long chatRoomId) {
        User teacher = getTeacherOrThrow(userId, activeRole);
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        boolean hasAccess = chatRoomRepository.existsParticipantByChatRoomIdAndUserIdAndParticipantRole(chatRoomId, userId, activeRole);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근 권한이 없습니다.");
        }

        List<Message> messages = messageRepository.findAllByChatRoomIdForReport(chatRoomId);
        byte[] pdfBytes = reportPdfRenderService.render(chatRoomId, chatRoom.getIntentLabel(), messages);

        String parentName = safeParentName(chatRoomRepository.findParentNameByChatRoomId(chatRoomId));
        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        String displayFileName = "증빙리포트_" + parentName + "_" + timestamp + ".pdf";
        String objectFileName = "report-" + UUID.randomUUID() + ".pdf";
        String objectKey = reportS3StorageService.buildObjectKey(chatRoomId, objectFileName);
        String s3Uri = reportS3StorageService.uploadPdf(objectKey, pdfBytes);

        PdfFile pdfFile = pdfFileRepository.save(PdfFile.create(chatRoom, teacher, s3Uri, displayFileName));
        String downloadUrl = reportS3StorageService.createDownloadUrl(objectKey);

        return new ReportPdfCreateData(
                pdfFile.getId(),
                pdfFile.getFileName(),
                downloadUrl,
                reportS3StorageService.getPresignedExpireSeconds()
        );
    }

    private User getTeacherOrThrow(Long userId, String activeRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_TEACHER.equalsIgnoreCase(activeRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 세션에서만 리포트를 조회할 수 있습니다.");
        }
        if (!user.hasRole(ROLE_TEACHER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 역할이 없는 계정입니다.");
        }
        return user;
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
