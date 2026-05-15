package com.softy.be.chat.service;

import com.softy.be.chat.dto.ChatRoomMessageListData;
import com.softy.be.chat.dto.ChatRoomMessageSendRequest;
import com.softy.be.chat.dto.ChatRoomStatusUpdateData;
import com.softy.be.chat.dto.ChatRoomStatusUpdateRequest;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.chat.dto.TeacherMessageSendRequest;
import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.ChatRoomUserMap;
import com.softy.be.chat.entity.AiRecommendation;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.entity.MessageAnalysis;
import com.softy.be.chat.repository.AiFeedbackRepository;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.ChatRoomUserMapRepository;
import com.softy.be.chat.repository.MessageAnalysisRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.entity.Student;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.TeacherSettingRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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

    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_PARENT = "PARENT";

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

        ChatRoomUserMap mapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomUserMapRepository.findByChatRoomIdAndUserId(15L, 1L)).thenReturn(Optional.of(mapping));

        ChatRoomStatusUpdateData result = chatRoomService.updateChatRoomStatus(
                1L,
                ROLE_TEACHER,
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

        assertThatThrownBy(() -> chatRoomService.updateChatRoomStatus(
                2L,
                ROLE_PARENT,
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
                ROLE_TEACHER,
                15L,
                new ChatRoomStatusUpdateRequest(null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(userRepository, chatRoomRepository, chatRoomUserMapRepository);
    }

    @Test
    void getChatRoomMessagesMarksAllUnreadMineMessages() {
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

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, LocalDateTime.of(2026, 5, 12, 9, 0));
        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, ROLE_PARENT, 2, LocalDateTime.of(2026, 5, 12, 10, 1));

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

        ChatRoomMessageListData result = chatRoomService.getChatRoomMessages(1L, ROLE_TEACHER, 15L, null, 30);

        assertThat(result.messages()).hasSize(3);
        assertThat(result.messages().get(0).messageId()).isEqualTo(101L);
        assertThat(result.messages().get(0).isUnreadByCounterpart()).isFalse();
        assertThat(result.messages().get(1).messageId()).isEqualTo(102L);
        assertThat(result.messages().get(1).isUnreadByCounterpart()).isTrue();
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

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, LocalDateTime.of(2026, 5, 12, 9, 0));
        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, ROLE_PARENT, 0, LocalDateTime.of(2026, 5, 12, 10, 5));

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

        ChatRoomMessageListData result = chatRoomService.getChatRoomMessages(1L, ROLE_TEACHER, 15L, null, 30);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages())
                .allSatisfy(message -> assertThat(message.isUnreadByCounterpart()).isFalse());
    }

    @Test
    void sendChatRoomMessagePreservesLeadingAndTrailingWhitespace() {
        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);

        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, ROLE_PARENT, 0, null);
        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(parentMapping, teacherMapping));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawContent = "  hello teacher  ";
        var result = chatRoomService.sendChatRoomMessage(2L, ROLE_PARENT, 15L, new ChatRoomMessageSendRequest(rawContent));

        assertThat(result.content()).isEqualTo(rawContent);
    }

    @Test
    void sendChatRoomMessageAllowsWhitespaceOnlyContent() {
        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);

        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, ROLE_PARENT, 0, null);
        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(parentMapping, teacherMapping));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawContent = "   ";
        var result = chatRoomService.sendChatRoomMessage(2L, ROLE_PARENT, 15L, new ChatRoomMessageSendRequest(rawContent));

        assertThat(result.content()).isEqualTo(rawContent);
    }

    @Test
    void sendTeacherMessagePreservesLeadingAndTrailingWhitespace() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, null);

        MessageAnalysis analysis = MessageAnalysis.create(
                chatRoom,
                teacher,
                "  original draft  ",
                "SAFE",
                null,
                LocalDateTime.of(2026, 5, 14, 10, 0)
        );
        ReflectionTestUtils.setField(analysis, "id", 30L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomUserMapRepository.findByChatRoomIdAndUserId(15L, 1L)).thenReturn(Optional.of(teacherMapping));
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(teacherMapping));
        when(messageAnalysisRepository.findById(30L)).thenReturn(Optional.of(analysis));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawContent = "  final teacher message  ";
        chatRoomService.sendTeacherMessage(1L, ROLE_TEACHER, 15L, new TeacherMessageSendRequest(30L, rawContent));

        assertThat(analysis.getUsedMessage()).isNotNull();
        assertThat(analysis.getUsedMessage().getModifyContent()).isEqualTo(rawContent);
    }

    @Test
    void sendTeacherMessageTreatsTrimmedMatchAsRecommendationUsed() {
        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        ChatRoom chatRoom = ChatRoom.create("CONSULT", ChatRoomStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(chatRoom, "id", 15L);

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, null);

        MessageAnalysis analysis = MessageAnalysis.create(
                chatRoom,
                teacher,
                "draft",
                "UNSAFE",
                "recommendation",
                LocalDateTime.of(2026, 5, 14, 10, 0)
        );
        ReflectionTestUtils.setField(analysis, "id", 31L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomUserMapRepository.findByChatRoomIdAndUserId(15L, 1L)).thenReturn(Optional.of(teacherMapping));
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(teacherMapping));
        when(messageAnalysisRepository.findById(31L)).thenReturn(Optional.of(analysis));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRecommendationRepository.save(any(AiRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatRoomService.sendTeacherMessage(1L, ROLE_TEACHER, 15L, new TeacherMessageSendRequest(31L, "  recommendation  "));

        ArgumentCaptor<AiRecommendation> recommendationCaptor = ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(recommendationCaptor.capture());
        AiRecommendation savedRecommendation = recommendationCaptor.getValue();
        assertThat(ReflectionTestUtils.getField(savedRecommendation, "content")).isEqualTo("recommendation");
        assertThat(ReflectionTestUtils.getField(savedRecommendation, "isRecommendationUsed")).isEqualTo(true);
    }

    @Test
    void analyzeInitMessageIntentPreservesLeadingAndTrailingWhitespace() {
        User parent = User.createForKakao("parent");
        parent.completeParentSignup("parent");
        ReflectionTestUtils.setField(parent, "id", 2L);

        User teacher = User.createForKakao("teacher");
        teacher.completeTeacherSignup("teacher");
        ReflectionTestUtils.setField(teacher, "id", 1L);

        Classroom classroom = Classroom.create(1, 1, null, teacher);
        Student student = Student.create("student", LocalDate.of(2020, 1, 1), "M", classroom);
        ParentStudent parentStudent = ParentStudent.create(parent, student);

        when(userRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(parentStudentRepository.findFirstByParentIdOrderByIdDesc(2L)).thenReturn(Optional.of(parentStudent));
        when(intentClassificationClient.classifyIntent("  need help  ")).thenReturn("CONSULT");

        var result = chatRoomService.analyzeInitMessageIntent(2L, ROLE_PARENT, new InitMessageIntentRequest("  need help  "));

        assertThat(result.intentLabel()).isEqualTo("CONSULT");
    }

    @Test
    void getChatRoomMessagesPreservesWhitespaceInDisplayedContent() {
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

        ChatRoomUserMap teacherMapping = ChatRoomUserMap.create(chatRoom, teacher, ROLE_TEACHER, 0, LocalDateTime.of(2026, 5, 12, 9, 0));
        ChatRoomUserMap parentMapping = ChatRoomUserMap.create(chatRoom, parent, ROLE_PARENT, 0, LocalDateTime.of(2026, 5, 12, 10, 5));

        Message teacherMessage = Message.createReviewed("TEXT", "original", "  reviewed message  ", false, chatRoom, teacher);
        ReflectionTestUtils.setField(teacherMessage, "id", 102L);
        ReflectionTestUtils.setField(teacherMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 3));

        Message parentMessage = Message.create("TEXT", "  parent message  ", chatRoom, parent);
        ReflectionTestUtils.setField(parentMessage, "id", 101L);
        ReflectionTestUtils.setField(parentMessage, "createdAt", LocalDateTime.of(2026, 5, 12, 10, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(chatRoomRepository.findById(15L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomRepository.existsParticipantByChatRoomIdAndUserId(15L, 1L)).thenReturn(true);
        when(chatRoomUserMapRepository.findAllByChatRoomId(15L)).thenReturn(List.of(teacherMapping, parentMapping));
        when(messageRepository.findPreviewMessages(eq(15L), eq(null), any())).thenReturn(List.of(teacherMessage, parentMessage));
        when(messageRepository.existsOlderMessage(15L, 101L)).thenReturn(false);

        ChatRoomMessageListData result = chatRoomService.getChatRoomMessages(1L, ROLE_TEACHER, 15L, null, 30);

        assertThat(result.messages()).extracting("content")
                .containsExactly("  parent message  ", "  reviewed message  ");
    }
}
