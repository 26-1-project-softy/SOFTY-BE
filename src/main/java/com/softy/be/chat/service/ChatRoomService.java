package com.softy.be.chat.service;

import com.softy.be.chat.dto.InitMessageIntentData;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final String ROLE_PARENT = "PARENT";

    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final IntentClassificationClient intentClassificationClient;

    @Transactional(readOnly = true)
    public InitMessageIntentData analyzeInitMessageIntent(Long userId, InitMessageIntentRequest request) {
        validateIntentRequest(request);
        resolveParentTeacherLink(userId);

        String intentLabel = intentClassificationClient.classifyIntent(request.content().trim());
        return new InitMessageIntentData(intentLabel);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ParentTeacherLink(
            User parent,
            User teacher
    ) {
    }
}
