package com.icegreen.greenmail.marketing.dto;

import com.icegreen.greenmail.marketing.model.enums.CampaignRoundStatus; // Required for status

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

// All fields optional for update
public class UpdateCampaignRoundRequest {

    private Long emailTemplateId;

    @Size(max = 255)
    private String subjectTemplate;

    @Min(value = 1, message = "Round number must be at least 1")
    @Max(value = 10, message = "Round number cannot exceed 10")
    private Integer roundNumber;

    @PositiveOrZero(message = "Time interval days must be zero or positive")
    private Integer timeIntervalDays;

    private LocalDateTime scheduledSendTime;

    private CampaignRoundStatus status; // Optional: status changes might have dedicated endpoints

    // Getters and Setters
    public Long getEmailTemplateId() {
        return emailTemplateId;
    }

    public void setEmailTemplateId(Long emailTemplateId) {
        this.emailTemplateId = emailTemplateId;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
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
}
