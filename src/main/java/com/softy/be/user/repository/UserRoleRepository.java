package com.softy.be.user.repository;

import com.softy.be.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    boolean existsByUserIdAndRole(Long userId, String role);
    List<UserRole> findAllByUserId(Long userId);
    Optional<UserRole> findFirstByUserIdAndRole(Long userId, String role);
    void deleteAllByUserId(Long userId);
}
