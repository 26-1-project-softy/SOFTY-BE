package com.softy.be.chat.service;

import com.softy.be.chat.dto.InitMessageIntentData;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.chat.dto.InitMessageSendData;
import com.softy.be.chat.dto.InitMessageSendRequest;
import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.ChatRoomUserMap;
import com.softy.be.chat.entity.Message;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final String ROLE_PARENT = "PARENT";
    private static final String MESSAGE_TYPE_TEXT = "TEXT";
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherSettingRepository teacherSettingRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserMapRepository chatRoomUserMapRepository;
    private final MessageRepository messageRepository;
    private final IntentClassificationClient intentClassificationClient;

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
        chatRoomUserMapRepository.save(ChatRoomUserMap.create(chatRoom, teacher, 1, now));

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "담당 교사 정보를 찾을 수 없습니다.");
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
