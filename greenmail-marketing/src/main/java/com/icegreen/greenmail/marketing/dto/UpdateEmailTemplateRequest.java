package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UpdateEmailTemplateRequest {

    @NotBlank(message = "Template name cannot be blank")
    @Size(max = 255)
    private String name;

    private String contentHtml;

    private String contentText;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }
}
