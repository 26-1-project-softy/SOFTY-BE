package com.softy.be.user.service;

import com.softy.be.auth.service.KakaoOAuthClient;
import com.softy.be.school.entity.Classroom;
import com.softy.be.school.entity.ParentStudent;
import com.softy.be.school.repository.ClassroomRepository;
import com.softy.be.school.repository.ParentStudentRepository;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.SocialAccountRepository;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private static final String KAKAO_PROVIDER = "KAKAO";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ClassroomRepository classroomRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final KakaoOAuthClient kakaoOAuthClient;

    @Transactional(readOnly = true)
    public UserMeResult getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        Integer grade = null;
        Integer classNumber = null;

        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            Classroom classroom = classroomRepository.findFirstByTeacherIdOrderByIdDesc(userId).orElse(null);
            if (classroom != null) {
                grade = classroom.getGrade();
                classNumber = classroom.getClassNumber();
            }
        } else if ("PARENT".equalsIgnoreCase(user.getRole())) {
            ParentStudent mapping = parentStudentRepository.findFirstByParentIdOrderByIdDesc(userId).orElse(null);
            if (mapping != null && mapping.getStudent() != null && mapping.getStudent().getClassroom() != null) {
                grade = mapping.getStudent().getClassroom().getGrade();
                classNumber = mapping.getStudent().getClassroom().getClassNumber();
            }
        }

        return new UserMeResult(user.getRole(), user.getName(), grade, classNumber);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        if ("WITHDRAWN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 탈퇴한 계정입니다");
        }

        socialAccountRepository.findFirstByUserIdAndProviderOrderByIdDesc(userId, KAKAO_PROVIDER)
                .ifPresent(socialAccount -> unlinkKakaoOrThrow(socialAccount.getProviderUserId()));

        socialAccountRepository.deleteAllByUserId(userId);
        user.withdraw();
    }

    private void unlinkKakaoOrThrow(String providerUserId) {
        try {
            kakaoOAuthClient.unlinkUserByAdminKey(providerUserId);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "카카오 연결 해제에 실패하여 회원 탈퇴를 중단했습니다.",
                    e
            );
        }
    }
}
