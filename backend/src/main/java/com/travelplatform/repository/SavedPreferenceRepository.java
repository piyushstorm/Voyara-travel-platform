package com.travelplatform.repository;

import com.travelplatform.entity.SavedPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedPreferenceRepository extends JpaRepository<SavedPreference, Long> {
    List<SavedPreference> findByUserIdOrderByCreatedAtDesc(Long userId);
}
