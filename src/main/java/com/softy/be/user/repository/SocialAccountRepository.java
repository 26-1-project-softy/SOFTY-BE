package com.softy.be.user.repository;

import com.softy.be.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<SocialAccount> findFirstByUserIdAndProviderOrderByIdDesc(Long userId, String provider);
    void deleteAllByUserId(Long userId);
}
