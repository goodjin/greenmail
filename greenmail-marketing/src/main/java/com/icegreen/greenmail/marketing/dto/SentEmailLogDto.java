package com.icegreen.greenmail.marketing.dto;

import com.icegreen.greenmail.marketing.model.enums.SentEmailStatus;
import java.time.LocalDateTime;

public class SentEmailLogDto {
    private Long id;
    private Long campaignRoundId;
    private Long contactId;
    private String contactEmail; // Denormalized for convenience
    private Long emailAccountId;
    private String sendingEmailAddress; // Denormalized for convenience
    private LocalDateTime sentAt;
    private SentEmailStatus status;
    private String errorMessage;
    private String messageIdHeader;
    private LocalDateTime createdAt; // Added for completeness
    private LocalDateTime updatedAt; // Added for completeness


    // Constructors, Getters, Setters
    public SentEmailLogDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCampaignRoundId() {
        return campaignRoundId;
    }

    public void setCampaignRoundId(Long campaignRoundId) {
        this.campaignRoundId = campaignRoundId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Long getEmailAccountId() {
        return emailAccountId;
    }

    public void setEmailAccountId(Long emailAccountId) {
        this.emailAccountId = emailAccountId;
    }

    public String getSendingEmailAddress() {
        return sendingEmailAddress;
    }

    public void setSendingEmailAddress(String sendingEmailAddress) {
        this.sendingEmailAddress = sendingEmailAddress;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public SentEmailStatus getStatus() {
        return status;
    }

    public void setStatus(SentEmailStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMessageIdHeader() {
        return messageIdHeader;
    }

    public void setMessageIdHeader(String messageIdHeader) {
        this.messageIdHeader = messageIdHeader;
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
