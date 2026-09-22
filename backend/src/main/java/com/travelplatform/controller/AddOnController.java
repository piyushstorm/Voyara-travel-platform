package com.travelplatform.controller;

import com.travelplatform.dto.ApiResponse;
import com.travelplatform.entity.AddOn;
import com.travelplatform.service.AddOnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addons")
@Tag(name = "Add-Ons", description = "Flight add-on services")
public class AddOnController {

    private final AddOnService addOnService;

    public AddOnController(AddOnService addOnService) {
        this.addOnService = addOnService;
    }

    @GetMapping
    @Operation(summary = "Get available add-ons for a cabin class")
    public ResponseEntity<ApiResponse<List<AddOn>>> getAddOns(
            @RequestParam(required = false) String cabinClass) {
        List<AddOn> addOns = addOnService.getAvailableAddOns(cabinClass);
        return ResponseEntity.ok(ApiResponse.success("Add-ons retrieved", addOns));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get add-on by ID")
    public ResponseEntity<ApiResponse<AddOn>> getAddOn(@PathVariable Long id) {
        AddOn addOn = addOnService.getAddOnById(id);
        return ResponseEntity.ok(ApiResponse.success("Add-on found", addOn));
    }
}
