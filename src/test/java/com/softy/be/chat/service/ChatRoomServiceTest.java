package com.softy.be.chat.service;

import com.softy.be.chat.dto.ChatRoomStatusUpdateData;
import com.softy.be.chat.dto.ChatRoomStatusUpdateRequest;
import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.ChatRoomUserMap;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
