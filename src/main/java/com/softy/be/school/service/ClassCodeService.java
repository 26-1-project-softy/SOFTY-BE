package com.softy.be.school.service;

import com.softy.be.school.entity.ClassCode;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.repository.ClassCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ClassCodeService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassCodeRepository classCodeRepository;

    @Transactional
    public String createClassCodeForClassroom(Classroom classroom) {
        classCodeRepository.deactivateActiveCodesByClassroomId(classroom.getId());

        String code = generateUniqueClassCode();
        classCodeRepository.save(ClassCode.create(code, classroom));
        return code;
    }

    private String generateUniqueClassCode() {
        for (int i = 0; i < 20; i++) {
            String candidate = randomCodeChunk(3) + "-" + randomCodeChunk(3);
            if (!classCodeRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "고유한 학급 코드를 생성하지 못했습니다");
    }

    private String randomCodeChunk(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            int index = RANDOM.nextInt(CODE_CHARS.length());
            builder.append(CODE_CHARS.charAt(index));
        }
        return builder.toString();
    }
}
