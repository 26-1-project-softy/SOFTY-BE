package com.softy.be.school.entity;

import com.softy.be.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate birthday;

    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    public static Student create(String name, LocalDate birthday, String gender, Classroom classroom) {
        Student student = new Student();
        student.name = name;
        student.birthday = birthday;
        student.gender = gender;
        student.classroom = classroom;
        return student;
    }

    public void updateClassroom(Classroom classroom) {
        this.classroom = classroom;
    }
}
