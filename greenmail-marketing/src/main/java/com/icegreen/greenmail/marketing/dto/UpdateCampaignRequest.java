package com.icegreen.greenmail.marketing.dto;

import com.icegreen.greenmail.marketing.model.enums.CampaignStatus; // Required for CampaignStatus
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

// All fields optional for update, except name if it's made mandatory by business logic
public class UpdateCampaignRequest {

    @NotBlank(message = "Campaign name cannot be blank") // Name is usually required even in updates
    @Size(max = 255)
    private String name;

    private String description;

    private List<Long> targetListIds; // Optional: if provided, will replace existing list

    private CampaignStatus status; // Optional: status changes might have dedicated endpoints

    // Getters and Setters
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

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }
}
