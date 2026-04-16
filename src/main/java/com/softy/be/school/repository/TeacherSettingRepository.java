package com.softy.be.school.repository;

import com.softy.be.school.entity.TeacherSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherSettingRepository extends JpaRepository<TeacherSetting, Long> {
    List<TeacherSetting> findByTeacherIdOrderByDayOfWeekAscIdAsc(Long teacherId);
    void deleteAllByTeacherId(Long teacherId);
}
