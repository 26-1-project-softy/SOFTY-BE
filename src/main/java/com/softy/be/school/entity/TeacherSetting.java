package com.softy.be.school.entity;

import com.softy.be.common.entity.BaseEntity;
import com.softy.be.user.entity.User;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_setting")
@Getter
@NoArgsConstructor
public class TeacherSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    public static TeacherSetting create(User teacher, short dayOfWeek, LocalDateTime startTime, LocalDateTime endTime) {
        TeacherSetting setting = new TeacherSetting();
        setting.teacher = teacher;
        setting.dayOfWeek = dayOfWeek;
        setting.startTime = startTime;
        setting.endTime = endTime;
        return setting;
    }
}
