package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

public class UpdateMemberRequest {

    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name; // Optional

    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email; // Optional

    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password; // Optional, for changing password

    // Constructors, Getters, Setters
    public UpdateMemberRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
