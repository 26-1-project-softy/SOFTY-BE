package com.softy.be.school.entity;

import com.softy.be.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "class_code")
@Getter
@NoArgsConstructor
public class ClassCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    public static ClassCode create(String code, Classroom classroom) {
        ClassCode classCode = new ClassCode();
        classCode.code = code;
        classCode.isActive = true;
        classCode.classroom = classroom;
        return classCode;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
