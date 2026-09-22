package com.travelplatform.repository;

import com.travelplatform.entity.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripExpenseRepository extends JpaRepository<TripExpense, Long> {
    List<TripExpense> findByGroupTripIdOrderByCreatedAtDesc(Long groupTripId);
}
