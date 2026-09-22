package com.travelplatform.repository;

import com.travelplatform.entity.AuthIdentity;
import com.travelplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long> {

    Optional<AuthIdentity> findByProviderAndProviderSubject(AuthIdentity.Provider provider, String providerSubject);

    List<AuthIdentity> findByUser(User user);

    List<AuthIdentity> findByUserId(Long userId);

    Optional<AuthIdentity> findByProviderAndUser(AuthIdentity.Provider provider, User user);

    Optional<AuthIdentity> findByPhoneNumber(String phoneNumber);

    boolean existsByProviderAndProviderSubject(AuthIdentity.Provider provider, String providerSubject);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByUserAndProvider(User user, AuthIdentity.Provider provider);
}
