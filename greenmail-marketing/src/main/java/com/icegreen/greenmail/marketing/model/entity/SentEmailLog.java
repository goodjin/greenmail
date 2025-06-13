package com.icegreen.greenmail.marketing.model.entity;

import com.icegreen.greenmail.marketing.model.enums.SentEmailStatus;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "sent_emails_log")
public class SentEmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_round_id", nullable = false)
    private CampaignRound campaignRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private TargetListContact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Column(name = "sent_at") // Nullable
    private Timestamp sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SentEmailStatus status;

    @Lob // For TEXT type
    @Column(name = "error_message") // Nullable
    private String errorMessage;

    @Column(name = "message_id_header") // Nullable
    private String messageIdHeader;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updatedAt;

    // Constructors
    public SentEmailLog() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CampaignRound getCampaignRound() {
        return campaignRound;
    }

    public void setCampaignRound(CampaignRound campaignRound) {
        this.campaignRound = campaignRound;
    }

    public TargetListContact getContact() {
        return contact;
    }

    public void setContact(TargetListContact contact) {
        this.contact = contact;
    }

    public EmailAccount getEmailAccount() {
        return emailAccount;
    }

    public void setEmailAccount(EmailAccount emailAccount) {
        this.emailAccount = emailAccount;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SentEmailLog that = (SentEmailLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SentEmailLog{" +
                "id=" + id +
                ", sentAt=" + sentAt +
                ", status=" + status +
                ", messageIdHeader='" + messageIdHeader + '\'' +
                // Foreign key entities excluded
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
