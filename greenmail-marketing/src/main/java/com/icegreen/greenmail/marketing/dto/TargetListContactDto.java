package com.icegreen.greenmail.marketing.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class TargetListContactDto {
    private Long id;
    private Long targetListId;
    private String emailAddress;
    private String firstName;
    private String lastName;
    private Map<String, Object> customFields; // For JSON
    private boolean subscribed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors, Getters, Setters
    public TargetListContactDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTargetListId() {
        return targetListId;
    }

    public void setTargetListId(Long targetListId) {
        this.targetListId = targetListId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
