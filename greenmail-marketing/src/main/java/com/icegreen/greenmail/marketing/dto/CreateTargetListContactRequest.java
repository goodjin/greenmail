package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

public class CreateTargetListContactRequest {

    @NotBlank(message = "Email address cannot be blank")
    @Email(message = "Email address should be valid")
    @Size(max = 255)
    private String emailAddress;

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    private Map<String, Object> customFields; // For JSON

    private Boolean subscribed; // Optional, defaults to true in entity

    // Getters and Setters
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public Boolean getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }
}
