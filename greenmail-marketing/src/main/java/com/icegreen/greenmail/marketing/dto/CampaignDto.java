package com.icegreen.greenmail.marketing.dto;

import com.icegreen.greenmail.marketing.model.enums.CampaignStatus;
import java.time.LocalDateTime;
import java.util.List;

public class CampaignDto {
    private Long id;
    private Long memberId;
    private String name;
    private String description;
    private CampaignStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TargetListDto> targetLists;
    private List<CampaignRoundDto> rounds;

    // Constructors, Getters, Setters
    public CampaignDto() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
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

    public List<TargetListDto> getTargetLists() {
        return targetLists;
    }

    public void setTargetLists(List<TargetListDto> targetLists) {
        this.targetLists = targetLists;
    }

    public List<CampaignRoundDto> getRounds() {
        return rounds;
    }

    public void setRounds(List<CampaignRoundDto> rounds) {
        this.rounds = rounds;
    }
}
