package com.icegreen.greenmail.marketing.dto;

import com.icegreen.greenmail.marketing.model.enums.CampaignRoundStatus;
import java.time.LocalDateTime;

public class CampaignRoundDto {
    private Long id;
    private Long campaignId;
    private int roundNumber;
    private Long emailTemplateId;
    private String emailTemplateName; // For convenience
    private String subjectTemplate;
    private Integer timeIntervalDays;
    private LocalDateTime scheduledSendTime;
    private CampaignRoundStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors, Getters, Setters
    public CampaignRoundDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Long getEmailTemplateId() {
        return emailTemplateId;
    }

    public void setEmailTemplateId(Long emailTemplateId) {
        this.emailTemplateId = emailTemplateId;
    }

    public String getEmailTemplateName() {
        return emailTemplateName;
    }

    public void setEmailTemplateName(String emailTemplateName) {
        this.emailTemplateName = emailTemplateName;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public Integer getTimeIntervalDays() {
        return timeIntervalDays;
    }

    public void setTimeIntervalDays(Integer timeIntervalDays) {
        this.timeIntervalDays = timeIntervalDays;
    }

    public LocalDateTime getScheduledSendTime() {
        return scheduledSendTime;
    }

    public void setScheduledSendTime(LocalDateTime scheduledSendTime) {
        this.scheduledSendTime = scheduledSendTime;
    }

    public CampaignRoundStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignRoundStatus status) {
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
}
