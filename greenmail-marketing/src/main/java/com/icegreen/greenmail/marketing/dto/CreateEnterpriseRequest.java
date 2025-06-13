package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CreateEnterpriseRequest {

    @NotBlank(message = "Enterprise name cannot be blank")
    @Size(min = 2, max = 255, message = "Enterprise name must be between 2 and 255 characters")
    private String name;

    // Constructors, Getters, Setters
    public CreateEnterpriseRequest() {
    }

    public CreateEnterpriseRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
