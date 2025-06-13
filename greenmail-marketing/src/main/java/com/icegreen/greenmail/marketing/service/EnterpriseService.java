package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import com.icegreen.greenmail.marketing.repository.EnterpriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;

    @Autowired
    public EnterpriseService(EnterpriseRepository enterpriseRepository) {
        this.enterpriseRepository = enterpriseRepository;
    }

    @Transactional
    public Enterprise createEnterprise(Enterprise enterprise) {
        // Basic validation can be added here if needed
        return enterpriseRepository.save(enterprise);
    }

    @Transactional(readOnly = true)
    public Enterprise getEnterpriseById(Long id) {
        return enterpriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enterprise", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Enterprise> getAllEnterprises() {
        return enterpriseRepository.findAll();
    }

    @Transactional
    public Enterprise updateEnterprise(Long id, Enterprise enterpriseDetails) {
        Enterprise existingEnterprise = getEnterpriseById(id); // Handles not found
        existingEnterprise.setName(enterpriseDetails.getName());
        // created_at and updated_at are handled by DB or JPA if configured with @UpdateTimestamp
        return enterpriseRepository.save(existingEnterprise);
    }

    @Transactional
    public void deleteEnterprise(Long id) {
        Enterprise enterprise = getEnterpriseById(id); // Handles not found
        enterpriseRepository.delete(enterprise);
    }
}
