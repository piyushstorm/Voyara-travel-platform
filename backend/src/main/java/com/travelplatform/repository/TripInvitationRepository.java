package com.travelplatform.repository;

import com.travelplatform.entity.TripInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TripInvitationRepository extends JpaRepository<TripInvitation, Long> {
    Optional<TripInvitation> findByToken(String token);
    List<TripInvitation> findByGroupTripIdAndStatus(Long groupTripId, String status);
    List<TripInvitation> findByInviteeEmailAndStatus(String email, String status);
    Optional<TripInvitation> findByGroupTripIdAndInviteeEmailAndStatus(Long groupTripId, String email, String status);
    List<TripInvitation> findByGroupTripIdAndInviteeEmail(Long groupTripId, String email);
}
