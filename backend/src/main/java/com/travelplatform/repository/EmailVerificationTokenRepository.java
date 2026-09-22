package com.travelplatform.repository;

import com.travelplatform.entity.EmailVerificationToken;
import com.travelplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, String status);
    Optional<EmailVerificationToken> findByTokenHashAndStatus(String tokenHash, String status);
    void deleteAllByUserAndStatus(User user, String status);
}
