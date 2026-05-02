package com.softy.be.chat.service;

import com.softy.be.chat.dto.ChatRoomDetailData;
import com.softy.be.chat.dto.ChatRoomListData;
import com.softy.be.chat.dto.ChatRoomListItemData;
import com.softy.be.chat.dto.ChatRoomMessageItemData;
import com.softy.be.chat.dto.ChatRoomMessageListData;
import com.softy.be.chat.dto.ChatRoomReadData;
import com.softy.be.chat.dto.InitMessageIntentData;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.chat.dto.InitMessageSendData;
import com.softy.be.chat.dto.InitMessageSendRequest;
import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.ChatRoomUserMap;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.repository.ChatRoomDetailRow;
import com.softy.be.chat.repository.ChatRoomListRow;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.ChatRoomUserMapRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.entity.TeacherSetting;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.school.repository.TeacherSettingRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final String ROLE_PARENT = "PARENT";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String MESSAGE_TYPE_TEXT = "TEXT";
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherSettingRepository teacherSettingRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserMapRepository chatRoomUserMapRepository;
    private final MessageRepository messageRepository;
    private final IntentClassificationClient intentClassificationClient;

    @Transactional(readOnly = true)
    public ChatRoomDetailData getChatRoomDetail(Long userId, Long chatRoomId) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_TEACHER.equalsIgnoreCase(user.getRole()) && !ROLE_PARENT.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 또는 교사 계정만 조회할 수 있습니다.");
        }

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        boolean hasAccess = chatRoomRepository.existsParticipantByChatRoomIdAndUserId(chatRoomId, userId);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다.");
        }

        ChatRoomDetailRow row = ROLE_TEACHER.equalsIgnoreCase(user.getRole())
                ? chatRoomRepository.findChatRoomDetailByTeacherIdAndChatRoomId(userId, chatRoomId)
                : chatRoomRepository.findChatRoomDetailByParentIdAndChatRoomId(userId, chatRoomId);

        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방 상세 정보를 찾을 수 없습니다.");
        }

        return new ChatRoomDetailData(
                row.getChatRoomId(),
                nullToEmpty(row.getCounterpartName()),
                nullToEmpty(row.getStudentName()),
                nullToEmpty(row.getIntentLabel()),
                nullToEmpty(row.getStatus())
        );
    }

    @Transactional
    public ChatRoomReadData markChatRoomAsRead(Long userId, Long chatRoomId) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_TEACHER.equalsIgnoreCase(user.getRole()) && !ROLE_PARENT.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 또는 교사 계정만 조회할 수 있습니다.");
        }

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        ChatRoomUserMap mapping = chatRoomUserMapRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        mapping.markAsRead(now);

        return new ChatRoomReadData(
                chatRoomId,
                mapping.getUnreadCount(),
                mapping.getLastReadAt()
        );
    }

    @Transactional(readOnly = true)
    public ChatRoomMessageListData getChatRoomMessages(Long userId, Long chatRoomId, Long cursor, int size) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }
        if (cursor != null && cursor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursor는 1 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~100 사이여야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_TEACHER.equalsIgnoreCase(user.getRole()) && !ROLE_PARENT.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 또는 교사 계정만 조회할 수 있습니다.");
        }

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        boolean hasAccess = chatRoomRepository.existsParticipantByChatRoomIdAndUserId(chatRoomId, userId);
        if (!hasAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다.");
        }

        List<Message> descendingMessages = messageRepository.findPreviewMessages(
                chatRoomId,
                cursor,
                PageRequest.of(0, size)
        );

        List<ChatRoomMessageItemData> messages = new ArrayList<>(descendingMessages.size());
        for (Message message : descendingMessages) {
            messages.add(new ChatRoomMessageItemData(
                    message.getId(),
                    message.getSender().getId().equals(userId),
                    nullToEmpty(message.getSender().getName()),
                    nullToEmpty(message.getSender().getRole()),
                    message.resolveReportContent(),
                    message.getCreatedAt()
            ));
        }
        Collections.reverse(messages);

        Long nextCursor = messages.isEmpty() ? null : messages.get(0).messageId();
        boolean hasNext = nextCursor != null && messageRepository.existsOlderMessage(chatRoomId, nextCursor);

        return new ChatRoomMessageListData(
                chatRoomId,
                messages,
                nextCursor,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public ChatRoomListData getChatRooms(Long userId, Long cursor, int size) {
        if (cursor != null && cursor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursor는 1 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~100 사이여야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<ChatRoomListRow> result;
        PageRequest pageable = PageRequest.of(0, size + 1);
        if (ROLE_TEACHER.equalsIgnoreCase(user.getRole())) {
            result = chatRoomRepository.findChatRoomsByTeacherId(userId, cursor, pageable);
        } else if (ROLE_PARENT.equalsIgnoreCase(user.getRole())) {
            result = chatRoomRepository.findChatRoomsByParentId(userId, cursor, pageable);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 또는 교사 계정만 조회할 수 있습니다.");
        }

        boolean hasNext = result.size() > size;
        List<ChatRoomListRow> pageContent = hasNext ? result.subList(0, size) : result;
        Long nextCursor = hasNext ? pageContent.get(pageContent.size() - 1).getChatRoomId() : null;

        return new ChatRoomListData(
                pageContent.stream()
                        .map(this::toChatRoomListItemData)
                        .toList(),
                size,
                nextCursor,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public InitMessageIntentData analyzeInitMessageIntent(Long userId, InitMessageIntentRequest request) {
        validateIntentRequest(request);
        ParentTeacherLink link = resolveParentTeacherLink(userId);

        String intentLabel = intentClassificationClient.classifyIntent(request.content().trim());
        boolean isInWorkingHours = isTeacherInWorkingHours(link.teacher().getId());
        return new InitMessageIntentData(intentLabel, isInWorkingHours);
    }

    @Transactional
    public InitMessageSendData sendInitMessage(Long userId, InitMessageSendRequest request) {
        validateSendRequest(request);

        ParentTeacherLink link = resolveParentTeacherLink(userId);
        User parent = link.parent();
        User teacher = link.teacher();

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(
                request.intentLabel().trim(),
                ChatRoomStatus.IN_PROGRESS
        ));

        LocalDateTime now = LocalDateTime.now();
        chatRoomUserMapRepository.save(ChatRoomUserMap.create(chatRoom, parent, 0, now));
        chatRoomUserMapRepository.save(ChatRoomUserMap.create(chatRoom, teacher, 1, null));

        Message message = messageRepository.save(Message.create(
                MESSAGE_TYPE_TEXT,
                request.content().trim(),
                chatRoom,
                parent
        ));

        return new InitMessageSendData(chatRoom.getId(), message.getId());
    }

    private ParentTeacherLink resolveParentTeacherLink(Long userId) {
        User parent = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_PARENT.equalsIgnoreCase(parent.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 계정만 요청할 수 있습니다.");
        }

        ParentStudent mapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "연결된 자녀 정보를 찾을 수 없습니다."));

        if (mapping.getStudent() == null
                || mapping.getStudent().getClassroom() == null
                || mapping.getStudent().getClassroom().getTeacher() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 교사 정보를 찾을 수 없습니다.");
        }

        return new ParentTeacherLink(parent, mapping.getStudent().getClassroom().getTeacher());
    }

    private void validateIntentRequest(InitMessageIntentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (isBlank(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content는 필수입니다.");
        }
    }

    private void validateSendRequest(InitMessageSendRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (isBlank(request.content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content는 필수입니다.");
        }
        if (isBlank(request.intentLabel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "intentLabel은 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ChatRoomListItemData toChatRoomListItemData(ChatRoomListRow row) {
        return new ChatRoomListItemData(
                row.getChatRoomId(),
                nullToEmpty(row.getCounterpartName()),
                nullToEmpty(row.getStudentName()),
                nullToEmpty(row.getLastMessage()),
                row.getLastMessageAt(),
                row.getUnreadCount() == null ? 0 : row.getUnreadCount(),
                nullToEmpty(row.getStatus()),
                nullToEmpty(row.getIntentLabel())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isTeacherInWorkingHours(Long teacherId) {
        List<TeacherSetting> settings = teacherSettingRepository.findByTeacherIdOrderByDayOfWeekAscIdAsc(teacherId);
        if (settings.isEmpty()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE_ID);
        DayOfWeek currentDayOfWeek = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        return settings.stream()
                .filter(setting -> setting.getDayOfWeek() == currentDayOfWeek.getValue())
                .anyMatch(setting -> {
                    LocalTime startTime = setting.getStartTime().toLocalTime();
                    LocalTime endTime = setting.getEndTime().toLocalTime();
                    return !currentTime.isBefore(startTime) && currentTime.isBefore(endTime);
                });
    }

    private record ParentTeacherLink(
            User parent,
            User teacher
    ) {
    }
}
