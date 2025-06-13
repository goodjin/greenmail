package com.icegreen.greenmail.marketing.model.entity;

import com.icegreen.greenmail.marketing.model.enums.CampaignRoundStatus;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "campaign_rounds",
    uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "round_number"}))
public class CampaignRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber; // Changed from INT UNSIGNED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_template_id", nullable = false)
    private EmailTemplate emailTemplate;

    @Column(name = "subject_template", nullable = false)
    private String subjectTemplate;

    @Column(name = "time_interval_days") // Nullable
    private Integer timeIntervalDays; // Changed from INT UNSIGNED

    @Column(name = "scheduled_send_time") // Nullable
    private Timestamp scheduledSendTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignRoundStatus status = CampaignRoundStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updatedAt;

    // Constructors
    public CampaignRound() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public EmailTemplate getEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(EmailTemplate emailTemplate) {
        this.emailTemplate = emailTemplate;
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

    public Timestamp getScheduledSendTime() {
        return scheduledSendTime;
    }

    public void setScheduledSendTime(Timestamp scheduledSendTime) {
        this.scheduledSendTime = scheduledSendTime;
    }

    public CampaignRoundStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignRoundStatus status) {
        this.status = status;
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
        CampaignRound that = (CampaignRound) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CampaignRound{" +
                "id=" + id +
                ", roundNumber=" + roundNumber +
                ", subjectTemplate='" + subjectTemplate + '\'' +
                ", status=" + status +
                // Campaign & EmailTemplate excluded
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
