package com.travelplatform.service;

import com.travelplatform.entity.AddOn;
import com.travelplatform.repository.AddOnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddOnService {

    private final AddOnRepository addOnRepository;

    public AddOnService(AddOnRepository addOnRepository) {
        this.addOnRepository = addOnRepository;
    }

    @Transactional(readOnly = true)
    public List<AddOn> getAvailableAddOns(String cabinClass) {
        if (cabinClass != null && !cabinClass.isEmpty()) {
            return addOnRepository.findByActiveTrueAndCabinClassIsNullOrCabinClass(cabinClass);
        }
        return addOnRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<AddOn> getAddOnsByCategory(String category) {
        return addOnRepository.findByActiveTrueAndCategory(category);
    }

    @Transactional(readOnly = true)
    public AddOn getAddOnById(Long id) {
        return addOnRepository.findById(id)
                .orElseThrow(() -> new com.travelplatform.exception.ResourceNotFoundException("AddOn", "id", id));
    }
}
