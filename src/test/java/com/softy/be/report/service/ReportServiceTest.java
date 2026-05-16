package com.softy.be.report.service;

import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.report.dto.ReportChatPreviewData;
import com.softy.be.report.repository.PdfFileRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final String ROLE_TEACHER = "TEACHER";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private PdfFileRepository pdfFileRepository;

    @Mock
    private ReportPdfRenderService reportPdfRenderService;

    @Mock
    private ReportS3StorageService reportS3StorageService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void createPdfRejectsTeacherWithoutChatRoomAccess() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserIdAndParticipantRole(99L, 1L, ROLE_TEACHER)).thenReturn(false);

        assertThatThrownBy(() -> reportService.createPdf(1L, ROLE_TEACHER, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(chatRoomRepository).existsParticipantByChatRoomIdAndUserIdAndParticipantRole(99L, 1L, ROLE_TEACHER);
        verifyNoInteractions(messageRepository, reportPdfRenderService, reportS3StorageService, pdfFileRepository);
    }

    @Test
    void getChatPreviewPreservesWhitespaceInDisplayedContent() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 99L);

        Message parentMessage = Message.create("TEXT", "  parent message  ", chatRoom, parent);
        ReflectionTestUtils.setField(parentMessage, "id", 101L);
        ReflectionTestUtils.setField(parentMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 0));

        Message teacherMessage = Message.createReviewed("TEXT", "draft", "   ", false, chatRoom, teacher);
        ReflectionTestUtils.setField(teacherMessage, "id", 102L);
        ReflectionTestUtils.setField(teacherMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserIdAndParticipantRole(99L, 1L, ROLE_TEACHER)).thenReturn(true);
        when(messageRepository.findPreviewMessages(org.mockito.ArgumentMatchers.eq(99L), org.mockito.ArgumentMatchers.eq(null), any()))
                .thenReturn(List.of(teacherMessage, parentMessage));
        when(messageRepository.existsOlderMessage(99L, 101L)).thenReturn(false);

        ReportChatPreviewData result = reportService.getChatPreview(1L, ROLE_TEACHER, 99L, null, 2);

        assertThat(result.messages()).extracting("content")
                .containsExactly("  parent message  ", "   ");
    }

    @Test
    void getChatPreviewRejectsTeacherSessionWithoutTeacherParticipantMapping() {
        User multiRoleUser = User.createForKakao("teacher-parent");
        multiRoleUser.completeTeacherSignup("teacher-parent");
        multiRoleUser.completeParentSignup("teacher-parent");
        ReflectionTestUtils.setField(multiRoleUser, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(multiRoleUser));
        when(chatRoomRepository.findById(99L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserIdAndParticipantRole(99L, 1L, ROLE_TEACHER))
                .thenReturn(false);

        assertThatThrownBy(() -> reportService.getChatPreview(1L, ROLE_TEACHER, 99L, null, 30))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
