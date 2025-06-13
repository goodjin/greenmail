package com.icegreen.greenmail.marketing.dto;

import java.time.LocalDateTime;

public class TargetListDto {
    private Long id;
    private Long memberId;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Could include count of contacts, etc. later

    // Constructors, Getters, Setters
    public TargetListDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
