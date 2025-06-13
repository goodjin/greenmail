package com.icegreen.greenmail.marketing.dto;

import com.icegreen.greenmail.marketing.model.enums.CampaignStatus;
import javax.validation.constraints.NotNull;

public class UpdateCampaignStatusRequest {

    @NotNull(message = "Campaign status cannot be null")
    private CampaignStatus status;

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }
}
