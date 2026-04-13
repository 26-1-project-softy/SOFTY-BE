package com.softy.be.domain.school;

import com.softy.be.domain.common.BaseEntity;
import com.softy.be.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Classroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int grade;

    @Column(name = "class_number")
    private int classNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    public static Classroom create(int grade, int classNumber, School school, User teacher) {
        Classroom classroom = new Classroom();
        classroom.grade = grade;
        classroom.classNumber = classNumber;
        classroom.school = school;
        classroom.teacher = teacher;
        return classroom;
    }
}
