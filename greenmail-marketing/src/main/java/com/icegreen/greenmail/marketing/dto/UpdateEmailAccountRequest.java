package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

// All fields are optional for update
public class UpdateEmailAccountRequest {

    @Email(message = "Email address should be valid")
    @Size(max = 255)
    private String emailAddress;

    @Size(max = 255)
    private String smtpHost;

    @Positive(message = "SMTP port must be positive")
    private Integer smtpPort;

    @Size(max = 255)
    private String smtpUsername;

    private String smtpPassword; // Raw password

    @Size(max = 50)
    private String smtpProtocol;

    // Constructors, Getters, Setters

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
