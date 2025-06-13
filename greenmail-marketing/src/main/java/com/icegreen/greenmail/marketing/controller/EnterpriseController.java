package com.icegreen.greenmail.marketing.controller;

import com.icegreen.greenmail.marketing.dto.CreateEnterpriseRequest;
import com.icegreen.greenmail.marketing.dto.EnterpriseDto;
import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import com.icegreen.greenmail.marketing.service.EnterpriseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/enterprises")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Autowired
    public EnterpriseController(EnterpriseService enterpriseService) {
        this.enterpriseService = enterpriseService;
    }

    // --- Mapper Methods ---
    public static EnterpriseDto toDto(Enterprise enterprise) {
        if (enterprise == null) return null;
        return new EnterpriseDto(
                enterprise.getId(),
                enterprise.getName(),
                enterprise.getCreatedAt() != null ? enterprise.getCreatedAt().toLocalDateTime() : null,
                enterprise.getUpdatedAt() != null ? enterprise.getUpdatedAt().toLocalDateTime() : null
        );
    }

    public static Enterprise toEntity(CreateEnterpriseRequest request) {
        if (request == null) return null;
        Enterprise enterprise = new Enterprise();
        enterprise.setName(request.getName());
        // createdAt and updatedAt are handled by DB or JPA
        return enterprise;
    }

    // --- Controller Methods ---

    @PostMapping
    public ResponseEntity<EnterpriseDto> createEnterprise(@Valid @RequestBody CreateEnterpriseRequest request) {
        Enterprise enterprise = toEntity(request);
        Enterprise createdEnterprise = enterpriseService.createEnterprise(enterprise);
        return new ResponseEntity<>(toDto(createdEnterprise), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnterpriseDto> getEnterpriseById(@PathVariable Long id) {
        Enterprise enterprise = enterpriseService.getEnterpriseById(id);
        return ResponseEntity.ok(toDto(enterprise));
    }

    @GetMapping
    public ResponseEntity<List<EnterpriseDto>> getAllEnterprises() {
        List<Enterprise> enterprises = enterpriseService.getAllEnterprises();
        List<EnterpriseDto> enterpriseDtos = enterprises.stream()
                .map(EnterpriseController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(enterpriseDtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnterpriseDto> updateEnterprise(@PathVariable Long id, @Valid @RequestBody CreateEnterpriseRequest request) {
        Enterprise enterpriseDetails = toEntity(request);
        Enterprise updatedEnterprise = enterpriseService.updateEnterprise(id, enterpriseDetails);
        return ResponseEntity.ok(toDto(updatedEnterprise));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnterprise(@PathVariable Long id) {
        enterpriseService.deleteEnterprise(id);
        return ResponseEntity.noContent().build();
    }
}
