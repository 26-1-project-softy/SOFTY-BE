package com.softy.be.user.entity;

import com.softy.be.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User extends BaseEntity {
    private static final String ROLE_UNASSIGNED = "UNASSIGNED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "login_id")
    private String loginId;

    private String pw;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private final List<UserRole> userRoles = new ArrayList<>();

    public static User createForKakao(String name) {
        User user = new User();
        user.name = name;
        return user;
    }

    public static User createAdmin(String name, String loginId, String encodedPassword) {
        User user = new User();
        user.name = name;
        user.loginId = loginId;
        user.pw = encodedPassword;
        user.addRole("ADMIN");
        return user;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void completeTeacherSignup(String name) {
        this.name = name;
        addRole("TEACHER");
    }

    public void completeParentSignup(String name) {
        this.name = name;
        addRole("PARENT");
    }

    public void withdraw() {
        this.name = "withdrawn_user_" + this.id;
        this.loginId = null;
        this.pw = null;
        this.userRoles.clear();
    }

    public void applyDevLoginProfile(String name, String role) {
        this.name = name;
        this.userRoles.clear();
        if (role != null && !ROLE_UNASSIGNED.equalsIgnoreCase(role)) {
            addRole(role);
        }
    }

    public boolean hasRole(String role) {
        String normalizedRole = normalizeRole(role);
        return userRoles.stream()
                .anyMatch(userRole -> normalizedRole.equals(userRole.getRole()));
    }

    public String getRole() {
        return userRoles.stream()
                .map(UserRole::getRole)
                .findFirst()
                .orElse(ROLE_UNASSIGNED);
    }

    public void addRole(String role) {
        String normalizedRole = normalizeRole(role);
        if (userRoles.stream().noneMatch(userRole -> normalizedRole.equals(userRole.getRole()))) {
            userRoles.add(UserRole.create(this, normalizedRole));
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
