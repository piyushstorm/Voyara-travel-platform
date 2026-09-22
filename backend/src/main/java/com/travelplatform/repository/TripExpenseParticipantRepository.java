package com.travelplatform.repository;

import com.travelplatform.entity.TripExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripExpenseParticipantRepository extends JpaRepository<TripExpenseParticipant, Long> {
    List<TripExpenseParticipant> findByExpenseId(Long expenseId);
    List<TripExpenseParticipant> findByUserId(Long userId);
}
