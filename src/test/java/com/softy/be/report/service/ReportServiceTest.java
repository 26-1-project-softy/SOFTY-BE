package com.softy.be.report.service;

import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.MessageRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

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
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserId(99L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> reportService.createPdf(1L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(chatRoomRepository).existsParticipantByChatRoomIdAndUserId(99L, 1L);
        verifyNoInteractions(messageRepository, reportPdfRenderService, reportS3StorageService, pdfFileRepository);
    }
}
