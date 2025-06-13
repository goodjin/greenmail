package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero; // For timeIntervalDays
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class CreateCampaignRoundRequest {

    @NotNull(message = "Email Template ID cannot be null")
    private Long emailTemplateId;

    @NotBlank(message = "Subject template cannot be blank")
    @Size(max = 255)
    private String subjectTemplate;

    @NotNull(message = "Round number cannot be null")
    @Min(value = 1, message = "Round number must be at least 1")
    @Max(value = 10, message = "Round number cannot exceed 10") // Example max, adjust as needed
    private Integer roundNumber; // Changed to Integer to allow @NotNull

    @PositiveOrZero(message = "Time interval days must be zero or positive")
    private Integer timeIntervalDays; // Days after previous round or campaign start; null if using scheduledSendTime

    private LocalDateTime scheduledSendTime; // Absolute send time; null if using timeIntervalDays

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
}
