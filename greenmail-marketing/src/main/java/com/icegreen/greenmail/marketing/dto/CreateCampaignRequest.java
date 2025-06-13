package com.icegreen.greenmail.marketing.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class CreateCampaignRequest {

    // @NotNull(message = "Member ID cannot be null")
    // private Long memberId; // Assuming explicit memberId for now

    @NotBlank(message = "Campaign name cannot be blank")
    @Size(max = 255)
    private String name;

    private String description;

    @NotEmpty(message = "At least one target list ID must be provided")
    private List<Long> targetListIds;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Long> getTargetListIds() {
        return targetListIds;
    }

    public void setTargetListIds(List<Long> targetListIds) {
        this.targetListIds = targetListIds;
    }
}
