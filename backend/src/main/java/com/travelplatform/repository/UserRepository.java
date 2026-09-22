package com.travelplatform.repository;

import com.travelplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Find all users with a password reset token set (for hashed token lookup) */
    List<User> findByPasswordResetTokenIsNotNull();
}
