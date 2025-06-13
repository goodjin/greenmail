package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class CreateEmailAccountRequest {

    // @NotNull(message = "Member ID cannot be null")
    // private Long memberId; // In a real app, this might come from authenticated principal

    @NotBlank(message = "Email address cannot be blank")
    @Email(message = "Email address should be valid")
    @Size(max = 255)
    private String emailAddress;

    @NotBlank(message = "SMTP host cannot be blank")
    @Size(max = 255)
    private String smtpHost;

    @NotNull(message = "SMTP port cannot be null")
    @Positive(message = "SMTP port must be positive")
    private Integer smtpPort;

    @NotBlank(message = "SMTP username cannot be blank")
    @Size(max = 255)
    private String smtpUsername;

    @NotBlank(message = "SMTP password cannot be blank")
    private String smtpPassword; // Raw password, service layer should handle encryption if needed for storage (though often stored as-is for SMTP client)

    @NotBlank(message = "SMTP protocol cannot be blank")
    @Size(max = 50)
    private String smtpProtocol; // e.g., "smtp", "smtps"

    // Constructors, Getters, Setters

    // public Long getMemberId() {
    //     return memberId;
    // }

    // public void setMemberId(Long memberId) {
    //     this.memberId = memberId;
    // }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpProtocol() {
        return smtpProtocol;
    }

    public void setSmtpProtocol(String smtpProtocol) {
        this.smtpProtocol = smtpProtocol;
    }
}
