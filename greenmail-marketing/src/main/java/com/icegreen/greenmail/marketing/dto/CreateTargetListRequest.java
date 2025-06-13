package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateTargetListRequest {

    // @NotNull(message = "Member ID cannot be null")
    // private Long memberId; // Assuming explicit memberId for now

    @NotBlank(message = "Target list name cannot be blank")
    @Size(max = 255)
    private String name;

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
}
