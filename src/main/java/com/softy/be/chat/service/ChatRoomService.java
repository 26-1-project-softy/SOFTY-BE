package com.softy.be.chat.service;

import com.softy.be.chat.dto.ChatRoomDetailData;
import com.softy.be.chat.dto.ChatRoomListData;
import com.softy.be.chat.dto.ChatRoomListItemData;
import com.softy.be.chat.dto.ChatRoomMessageItemData;
import com.softy.be.chat.dto.ChatRoomMessageListData;
import com.softy.be.chat.dto.ChatRoomMessageSendData;
import com.softy.be.chat.dto.ChatRoomMessageSendRequest;
import com.softy.be.chat.dto.ChatRoomReadData;
import com.softy.be.chat.dto.ChatRoomStatusUpdateData;
import com.softy.be.chat.dto.ChatRoomStatusUpdateRequest;
import com.softy.be.chat.dto.InitMessageIntentData;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.chat.dto.InitMessageSendData;
import com.softy.be.chat.dto.InitMessageSendRequest;
import com.softy.be.chat.dto.TeacherMessageAnalyzeFeedbackRequest;
import com.softy.be.chat.dto.TeacherMessageAnalyzeData;
import com.softy.be.chat.dto.TeacherMessageAnalyzeRequest;
import com.softy.be.chat.dto.TeacherMessageSendData;
import com.softy.be.chat.dto.TeacherMessageSendRequest;
import com.softy.be.chat.dto.TeacherWorkingHoursStatusData;
import com.softy.be.chat.entity.AiFeedback;
import com.softy.be.chat.entity.AiRecommendation;
import com.softy.be.chat.entity.ChatRoom;
import com.softy.be.chat.entity.ChatRoomStatus;
import com.softy.be.chat.entity.ChatRoomUserMap;
import com.softy.be.chat.entity.Message;
import com.softy.be.chat.entity.MessageAnalysis;
import com.softy.be.chat.repository.ChatRoomDetailRow;
import com.softy.be.chat.repository.ChatRoomListRow;
import com.softy.be.chat.repository.AiFeedbackRepository;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.ChatRoomRepository;
import com.softy.be.chat.repository.ChatRoomUserMapRepository;
import com.softy.be.chat.repository.MessageAnalysisRepository;
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
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final String ROLE_PARENT = "PARENT";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String MESSAGE_TYPE_TEXT = "TEXT";
    private static final String AI_FEEDBACK_TYPE_RISK_ANALYSIS = "RISK_ANALYSIS";
    private static final String RISK_LEVEL_UNSAFE = "UNSAFE";
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherSettingRepository teacherSettingRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserMapRepository chatRoomUserMapRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final MessageAnalysisRepository messageAnalysisRepository;
    private final MessageRepository messageRepository;
    private final IntentClassificationClient intentClassificationClient;
    private final TeacherMessageAnalysisClient teacherMessageAnalysisClient;

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
    public ChatRoomStatusUpdateData updateChatRoomStatus(Long userId, Long chatRoomId, ChatRoomStatusUpdateRequest request) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }
        validateChatRoomStatusUpdateRequest(request);

        getTeacherUser(userId);
        ChatRoomUserMap mapping = getChatRoomParticipantMapping(chatRoomId, userId);
        ChatRoom chatRoom = mapping.getChatRoom();
        chatRoom.updateStatus(request.status());

        return new ChatRoomStatusUpdateData(chatRoom.getId(), chatRoom.getStatus().name());
    }

    @Transactional
    public TeacherMessageAnalyzeData analyzeTeacherMessage(Long userId, Long chatRoomId, TeacherMessageAnalyzeRequest request) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }
        validateTeacherMessageAnalyzeRequest(request);

        User teacher = getTeacherUser(userId);
        ChatRoomUserMap mapping = getChatRoomParticipantMapping(chatRoomId, userId);
        return analyzeTeacherMessageContent(teacher, mapping.getChatRoom(), request.content());
    }

    @Transactional
    public TeacherMessageAnalyzeData recheckTeacherMessage(Long userId, Long analysisId, TeacherMessageAnalyzeRequest request) {
        if (analysisId == null || analysisId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId는 1 이상이어야 합니다.");
        }
        validateTeacherMessageAnalyzeRequest(request);

        User teacher = getTeacherUser(userId);
        MessageAnalysis baseAnalysis = messageAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지 분석 결과를 찾을 수 없습니다."));

        if (!baseAnalysis.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 메시지 분석 결과만 재분석할 수 있습니다.");
        }

        return analyzeTeacherMessageContent(teacher, baseAnalysis.getChatRoom(), request.content());
    }

    @Transactional
    public void saveTeacherMessageAnalyzeFeedback(Long userId, Long analysisId, TeacherMessageAnalyzeFeedbackRequest request) {
        if (analysisId == null || analysisId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId는 1 이상이어야 합니다.");
        }
        validateTeacherMessageAnalyzeFeedbackRequest(request);

        User teacher = getTeacherUser(userId);
        MessageAnalysis analysis = messageAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지 분석 결과를 찾을 수 없습니다."));

        if (!analysis.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 메시지 분석 결과에만 피드백을 남길 수 있습니다.");
        }

        AiFeedback feedback = aiFeedbackRepository.findFirstByMessageAnalysisId(analysis.getId())
                .orElseGet(() -> AiFeedback.create(analysis, AI_FEEDBACK_TYPE_RISK_ANALYSIS, request.score()));

        feedback.updateScore(request.score());
        aiFeedbackRepository.save(feedback);
    }

    @Transactional
    public void saveTeacherMessageRecommendationAdoption(Long userId, Long analysisId) {
        if (analysisId == null || analysisId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId는 1 이상이어야 합니다.");
        }

        User teacher = getTeacherUser(userId);
        MessageAnalysis analysis = messageAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지 분석 결과를 찾을 수 없습니다."));

        if (!analysis.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 메시지 분석 결과에만 추천문장 적용을 기록할 수 있습니다.");
        }
        if (isBlank(analysis.getRecommendedMessage())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "추천문장이 없는 분석 결과에는 적용을 기록할 수 없습니다.");
        }

        analysis.markRecommendationAdopted();
    }

    @Transactional
    public TeacherMessageSendData sendTeacherMessage(Long userId, Long chatRoomId, TeacherMessageSendRequest request) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }
        validateTeacherMessageSendRequest(request);

        User teacher = getTeacherUser(userId);
        ChatRoomUserMap senderMapping = getChatRoomParticipantMapping(chatRoomId, userId);
        MessageAnalysis analysis = messageAnalysisRepository.findById(request.analysisId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지 분석 결과를 찾을 수 없습니다."));

        if (!analysis.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 메시지 분석 결과로만 메시지를 전송할 수 있습니다.");
        }
        if (!analysis.getChatRoom().getId().equals(chatRoomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 채팅방에 속한 분석 결과로만 메시지를 전송할 수 있습니다.");
        }
        if (analysis.getUsedMessage() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용된 분석 결과로는 다시 메시지를 전송할 수 없습니다.");
        }

        String finalContent = request.content();
        Message message = messageRepository.save(Message.createReviewed(
                MESSAGE_TYPE_TEXT,
                analysis.getOriginalContent(),
                finalContent,
                RISK_LEVEL_UNSAFE.equalsIgnoreCase(analysis.getRiskLevel()),
                senderMapping.getChatRoom(),
                teacher
        ));

        analysis.linkUsedMessage(message);

        String recommendedMessage = analysis.getRecommendedMessage();
        if (!isBlank(recommendedMessage)) {
            boolean isRecommendationUsed = recommendedMessage.trim().equals(finalContent.trim());
            aiRecommendationRepository.save(AiRecommendation.create(
                    message,
                    recommendedMessage,
                    isRecommendationUsed
            ));
        }

        LocalDateTime now = LocalDateTime.now();
        senderMapping.markAsRead(now);
        chatRoomUserMapRepository.findAllByChatRoomId(chatRoomId).stream()
                .filter(mapping -> !mapping.getUser().getId().equals(userId))
                .forEach(ChatRoomUserMap::increaseUnreadCount);

        return new TeacherMessageSendData(message.getId(), chatRoomId);
    }

    @Transactional
    public ChatRoomMessageSendData sendChatRoomMessage(Long userId, Long chatRoomId, ChatRoomMessageSendRequest request) {
        if (chatRoomId == null || chatRoomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chatRoomId는 1 이상이어야 합니다.");
        }
        validateChatRoomMessageSendRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_PARENT.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학부모 계정만 메시지를 전송할 수 있습니다.");
        }

        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        List<ChatRoomUserMap> mappings = chatRoomUserMapRepository.findAllByChatRoomId(chatRoomId);
        ChatRoomUserMap senderMapping = mappings.stream()
                .filter(mapping -> mapping.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다."));

        ChatRoomUserMap receiverMapping = mappings.stream()
                .filter(mapping -> !mapping.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 받을 상대방을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        senderMapping.markAsRead(now);
        receiverMapping.increaseUnreadCount();

        Message message = messageRepository.save(Message.create(
                MESSAGE_TYPE_TEXT,
                request.content(),
                senderMapping.getChatRoom(),
                user
        ));

        return new ChatRoomMessageSendData(
                message.getId(),
                senderMapping.getChatRoom().getId(),
                message.getContent(),
                message.getCreatedAt()
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

        LocalDateTime counterpartLastReadAt = chatRoomUserMapRepository.findAllByChatRoomId(chatRoomId).stream()
                .filter(mapping -> !mapping.getUser().getId().equals(userId))
                .findFirst()
                .map(ChatRoomUserMap::getLastReadAt)
                .orElse(null);

        List<Message> descendingMessages = messageRepository.findPreviewMessages(
                chatRoomId,
                cursor,
                PageRequest.of(0, size)
        );

        List<ChatRoomMessageItemData> messages = new ArrayList<>(descendingMessages.size());
        Long latestUnreadMessageId = findLatestUnreadMessageId(descendingMessages, userId, counterpartLastReadAt);
        for (Message message : descendingMessages) {
            boolean isMine = message.getSender().getId().equals(userId);
            messages.add(new ChatRoomMessageItemData(
                    message.getId(),
                    isMine,
                    nullToEmpty(message.getSender().getName()),
                    nullToEmpty(message.getSender().getRole()),
                    message.resolveReportContent(),
                    message.getCreatedAt(),
                    isMine && message.getId().equals(latestUnreadMessageId)
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
        resolveParentTeacherLink(userId);

        String intentLabel = intentClassificationClient.classifyIntent(request.content());
        return new InitMessageIntentData(intentLabel);
    }

    @Transactional(readOnly = true)
    public TeacherWorkingHoursStatusData getTeacherWorkingHoursStatus(Long userId) {
        ParentTeacherLink link = resolveParentTeacherLink(userId);
        return new TeacherWorkingHoursStatusData(isTeacherInWorkingHours(link.teacher().getId()));
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
                request.content(),
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

    private User getTeacherUser(Long userId) {
        User teacher = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ROLE_TEACHER.equalsIgnoreCase(teacher.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교사 계정만 요청할 수 있습니다.");
        }
        return teacher;
    }

    private ChatRoomUserMap getChatRoomParticipantMapping(Long chatRoomId, Long userId) {
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        return chatRoomUserMapRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다."));
    }

    private void validateIntentRequest(InitMessageIntentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
    }

    private void validateSendRequest(InitMessageSendRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (isBlank(request.intentLabel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "intentLabel은 필수입니다.");
        }
    }

    private void validateTeacherMessageAnalyzeRequest(TeacherMessageAnalyzeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
    }

    private void validateChatRoomMessageSendRequest(ChatRoomMessageSendRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
    }

    private void validateChatRoomStatusUpdateRequest(ChatRoomStatusUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status는 필수입니다.");
        }
    }

    private void validateTeacherMessageAnalyzeFeedbackRequest(TeacherMessageAnalyzeFeedbackRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (request.score() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score는 필수입니다.");
        }
        if (request.score() < 1 || request.score() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score는 1부터 5 사이여야 합니다.");
        }
    }

    private void validateTeacherMessageSendRequest(TeacherMessageSendRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }
        if (request.analysisId() == null || request.analysisId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "analysisId는 1 이상이어야 합니다.");
        }
    }

    private TeacherMessageAnalyzeData analyzeTeacherMessageContent(User teacher, ChatRoom chatRoom, String originalContent) {
        String riskLevel = teacherMessageAnalysisClient.detectRisk(originalContent);
        if (isBlank(riskLevel)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 분쟁 가능성 분석에 실패했습니다.");
        }

        String normalizedRiskLevel = riskLevel.trim().toUpperCase(Locale.ROOT);
        String recommendedMessage = null;
        if (RISK_LEVEL_UNSAFE.equals(normalizedRiskLevel)) {
            recommendedMessage = teacherMessageAnalysisClient.recommendAlternative(originalContent);
            if (isBlank(recommendedMessage)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 추천 문장 생성에 실패했습니다.");
            }
            recommendedMessage = recommendedMessage.trim();
        }

        MessageAnalysis analysis = messageAnalysisRepository.save(MessageAnalysis.create(
                chatRoom,
                teacher,
                originalContent,
                normalizedRiskLevel,
                recommendedMessage,
                LocalDateTime.now().plusMinutes(30)
        ));

        return new TeacherMessageAnalyzeData(
                analysis.getId(),
                analysis.getRiskLevel(),
                analysis.getRecommendedMessage()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long findLatestUnreadMessageId(List<Message> descendingMessages, Long userId, LocalDateTime counterpartLastReadAt) {
        for (Message message : descendingMessages) {
            if (!message.getSender().getId().equals(userId)) {
                continue;
            }
            if (counterpartLastReadAt == null || message.getCreatedAt().isAfter(counterpartLastReadAt)) {
                return message.getId();
            }
        }
        return null;
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
                    LocalTime startTime = setting.getStartTime();
                    LocalTime endTime = setting.getEndTime();
                    return !currentTime.isBefore(startTime) && currentTime.isBefore(endTime);
                });
    }

    private record ParentTeacherLink(
            User parent,
            User teacher
    ) {
    }
}
