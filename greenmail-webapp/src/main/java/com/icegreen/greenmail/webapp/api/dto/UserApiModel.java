package com.icegreen.greenmail.webapp.api.dto;

public class UserApiModel {
    private String email;
    // private String login; // Consider if login is different from email and needed

    public UserApiModel() {
    }

    public UserApiModel(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
