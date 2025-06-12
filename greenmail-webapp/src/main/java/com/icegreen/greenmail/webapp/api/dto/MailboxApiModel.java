package com.icegreen.greenmail.webapp.api.dto;

public class MailboxApiModel {
    private String id; // Fully qualified name, e.g., user@domain#INBOX
    private String name; // Simple name, e.g., INBOX
    private String userEmail;

    public MailboxApiModel() {
    }

    public MailboxApiModel(String id, String name, String userEmail) {
        this.id = id;
        this.name = name;
        this.userEmail = userEmail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
