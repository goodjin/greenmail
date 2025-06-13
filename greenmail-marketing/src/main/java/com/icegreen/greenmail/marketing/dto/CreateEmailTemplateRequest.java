package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull; // Added for memberId
import javax.validation.constraints.Size;

public class CreateEmailTemplateRequest {

    // Member ID will be derived from authenticated principal
    // @NotNull(message = "Member ID cannot be null")
    // private Long memberId;

    @NotBlank(message = "Template name cannot be blank")
    @Size(max = 255)
    private String name;

    private String contentHtml; // Can be blank/null

    private String contentText; // Can be blank/null

    // Getters and Setters
    // public Long getMemberId() {
    //     return memberId;
    // }

    // public void setMemberId(Long memberId) {
    //     this.memberId = memberId;
    // }

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
