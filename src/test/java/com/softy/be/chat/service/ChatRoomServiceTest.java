package com.softy.be.chat.service;

import com.softy.be.chat.dto.ChatRoomMessageListData;
import com.softy.be.chat.dto.ChatRoomStatusUpdateData;
import com.softy.be.chat.dto.ChatRoomStatusUpdateRequest;
import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.ChatRoomUserMap;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.repository.AiFeedbackRepository;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.ChatRoomUserMapRepository;
import com.softy.be.chat.repository.MessageAnalysisRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.TeacherSettingRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParentStudentRepository parentStudentRepository;

    @Mock
    private TeacherSettingRepository teacherSettingRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomUserMapRepository chatRoomUserMapRepository;

    @Mock
    private AiFeedbackRepository aiFeedbackRepository;

    @Mock
    private AiRecommendationRepository aiRecommendationRepository;

    @Mock
    private MessageAnalysisRepository messageAnalysisRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private IntentClassificationClient intentClassificationClient;

    @Mock
    private TeacherMessageAnalysisClient teacherMessageAnalysisClient;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    void updateChatRoomStatusUpdatesStatusForTeacherParticipant() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap mapping = ChatRoomUserMap.create(chatRoom, teacher, 0, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomUserMapRepository.findByChatRoomIdAndUserId(15L, 1L)).thenReturn(Optional.of(mapping));

        ChatRoomStatusUpdateData result = chatRoomService.updateChatRoomStatus(
                1L,
                15L,
                new ChatRoomStatusUpdateRequest(ChatRoomStatus.COMPLETED)
        );

        assertThat(result.chatRoomId()).isEqualTo(15L);
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(chatRoom.getStatus()).isEqualTo(ChatRoomStatus.COMPLETED);

        verify(chatRoomRepository).findById(15L);
        verify(chatRoomUserMapRepository).findByChatRoomIdAndUserId(15L, 1L);
    }

    @Test
    void updateChatRoomStatusRejectsNonTeacherUser() {
        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> chatRoomService.updateChatRoomStatus(
                2L,
                15L,
                new ChatRoomStatusUpdateRequest(ChatRoomStatus.COMPLETED)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(chatRoomRepository, chatRoomUserMapRepository);
    }

    @Test
    void updateChatRoomStatusRejectsMissingStatus() {
        assertThatThrownBy(() -> chatRoomService.updateChatRoomStatus(
                1L,
                15L,
                new ChatRoomStatusUpdateRequest(null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(userRepository, chatRoomRepository, chatRoomUserMapRepository);
    }

    @Test
    void getChatRoomMessagesMarksOnlyLatestUnreadMineMessage() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);
        ReflectionTestUtils.setField(teacher, "name", "Teacher");

        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);
        ReflectionTestUtils.setField(parent, "name", "Parent");

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, 0, LocalDateTime.of(2026, 5, 12, 9, 0));
        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, 2, LocalDateTime.of(2026, 5, 12, 10, 1));

        Message latestTeacherMessage = Message.create("TEXT", "latest", chatRoom, teacher);
        ReflectionTestUtils.setField(latestTeacherMessage, "id", 103L);
        ReflectionTestUtils.setField(latestTeacherMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 3));

        Message olderTeacherMessage = Message.create("TEXT", "older", chatRoom, teacher);
        ReflectionTestUtils.setField(olderTeacherMessage, "id", 102L);
        ReflectionTestUtils.setField(olderTeacherMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 2));

        Message parentMessage = Message.create("TEXT", "reply", chatRoom, parent);
        ReflectionTestUtils.setField(parentMessage, "id", 101L);
        ReflectionTestUtils.setField(parentMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserId(15L, 1L)).thenReturn(true);
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(teacherMapping, parentMapping));
        when(messageRepository.findPreviewMessages(eq(15L), eq(null), any())).thenReturn(List.of(latestTeacherMessage, olderTeacherMessage, parentMessage));
        when(messageRepository.existsOlderMessage(15L, 101L)).thenReturn(false);

        ChatRoomMessageListData result = chatRoomService.getChatRoomMessages(1L, 15L, null, 30);

        assertThat(result.messages()).hasSize(3);
        assertThat(result.messages().get(0).messageId()).isEqualTo(101L);
        assertThat(result.messages().get(0).isUnreadByCounterpart()).isFalse();
        assertThat(result.messages().get(1).messageId()).isEqualTo(102L);
        assertThat(result.messages().get(1).isUnreadByCounterpart()).isFalse();
        assertThat(result.messages().get(2).messageId()).isEqualTo(103L);
        assertThat(result.messages().get(2).isUnreadByCounterpart()).isTrue();
    }

    @Test
    void getChatRoomMessagesReturnsNoUnreadMarkerWhenCounterpartAlreadyReadLatestMineMessage() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);
        ReflectionTestUtils.setField(teacher, "name", "Teacher");

        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);
        ReflectionTestUtils.setField(parent, "name", "Parent");

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, 0, LocalDateTime.of(2026, 5, 12, 9, 0));
        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, 0, LocalDateTime.of(2026, 5, 12, 10, 5));

        Message teacherMessage = Message.create("TEXT", "done", chatRoom, teacher);
        ReflectionTestUtils.setField(teacherMessage, "id", 102L);
        ReflectionTestUtils.setField(teacherMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 3));

        Message parentMessage = Message.create("TEXT", "reply", chatRoom, parent);
        ReflectionTestUtils.setField(parentMessage, "id", 101L);
        ReflectionTestUtils.setField(parentMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserId(15L, 1L)).thenReturn(true);
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(teacherMapping, parentMapping));
        when(messageRepository.findPreviewMessages(eq(15L), eq(null), any())).thenReturn(List.of(teacherMessage, parentMessage));
        when(messageRepository.existsOlderMessage(15L, 101L)).thenReturn(false);

        ChatRoomMessageListData result = chatRoomService.getChatRoomMessages(1L, 15L, null, 30);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages())
                .allSatisfy(message -> assertThat(message.isUnreadByCounterpart()).isFalse());
    }
}
