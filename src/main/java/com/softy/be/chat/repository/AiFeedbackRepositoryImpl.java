package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiFeedback;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiFeedbackRepositoryImpl implements AiFeedbackRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<AiFeedbackListRow> findRiskFeedbacks(
            String riskLevel,
            Integer feedbackResult,
            String teacherName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<AiFeedbackListRowData> contentQuery = criteriaBuilder.createQuery(AiFeedbackListRowData.class);
        Root<AiFeedback> feedbackRoot = contentQuery.from(AiFeedback.class);
        Join<Object, Object> messageAnalysisJoin = feedbackRoot.join("messageAnalysis");
        Join<Object, Object> teacherJoin = messageAnalysisJoin.join("teacher");

        List<Predicate> predicates = createPredicates(
                criteriaBuilder,
                feedbackRoot,
                messageAnalysisJoin,
                teacherJoin,
                riskLevel,
                feedbackResult,
                teacherName,
                startDateTime,
                endDateTime
        );

        contentQuery.select(criteriaBuilder.construct(
                AiFeedbackListRowData.class,
                feedbackRoot.get("id"),
                teacherJoin.get("name"),
                feedbackRoot.get("actualRiskScore"),
                messageAnalysisJoin.get("riskLevel"),
                messageAnalysisJoin.get("originalContent"),
                feedbackRoot.get("createdAt")
        ));
        contentQuery.where(predicates.toArray(Predicate[]::new));
        contentQuery.orderBy(
                criteriaBuilder.desc(feedbackRoot.get("createdAt")),
                criteriaBuilder.desc(feedbackRoot.get("id"))
        );

        TypedQuery<AiFeedbackListRowData> typedQuery = entityManager.createQuery(contentQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<AiFeedback> countRoot = countQuery.from(AiFeedback.class);
        Join<Object, Object> countMessageAnalysisJoin = countRoot.join("messageAnalysis");
        Join<Object, Object> countTeacherJoin = countMessageAnalysisJoin.join("teacher");

        List<Predicate> countPredicates = createPredicates(
                criteriaBuilder,
                countRoot,
                countMessageAnalysisJoin,
                countTeacherJoin,
                riskLevel,
                feedbackResult,
                teacherName,
                startDateTime,
                endDateTime
        );

        countQuery.select(criteriaBuilder.count(countRoot));
        countQuery.where(countPredicates.toArray(Predicate[]::new));

        List<AiFeedbackListRow> content = new ArrayList<>(typedQuery.getResultList());
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> createPredicates(
            CriteriaBuilder criteriaBuilder,
            Root<AiFeedback> feedbackRoot,
            Join<Object, Object> messageAnalysisJoin,
            Join<Object, Object> teacherJoin,
            String riskLevel,
            Integer feedbackResult,
            String teacherName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (riskLevel != null) {
            predicates.add(criteriaBuilder.equal(messageAnalysisJoin.get("riskLevel"), riskLevel));
        }
        if (feedbackResult != null) {
            predicates.add(criteriaBuilder.equal(feedbackRoot.get("actualRiskScore"), feedbackResult));
        }
        if (teacherName != null) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(teacherJoin.get("name")), teacherName));
        }
        if (startDateTime != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(feedbackRoot.get("createdAt"), startDateTime));
        }
        if (endDateTime != null) {
            predicates.add(criteriaBuilder.lessThan(feedbackRoot.get("createdAt"), endDateTime));
        }

        return predicates;
    }

    public record AiFeedbackListRowData(
            Long feedbackId,
            String teacherName,
            Integer feedbackResult,
            String riskLevel,
            String originalMessage,
            LocalDateTime createdAt
    ) implements AiFeedbackListRow {

        @Override
        public Long getFeedbackId() {
            return feedbackId;
        }

        @Override
        public String getTeacherName() {
            return teacherName;
        }

        @Override
        public Integer getFeedbackResult() {
            return feedbackResult;
        }

        @Override
        public String getRiskLevel() {
            return riskLevel;
        }

        @Override
        public String getOriginalMessage() {
            return originalMessage;
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
