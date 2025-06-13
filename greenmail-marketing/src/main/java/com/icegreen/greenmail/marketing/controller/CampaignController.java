package com.icegreen.greenmail.marketing.controller;

import com.icegreen.greenmail.marketing.dto.*;
import com.icegreen.greenmail.marketing.model.entity.*; // For Member
import com.icegreen.greenmail.marketing.service.CampaignSchedulingService;
import com.icegreen.greenmail.marketing.service.CampaignService;
import com.icegreen.greenmail.marketing.service.MemberService; // For principal
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // For principal
import org.springframework.security.core.userdetails.UserDetails; // For principal
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;


import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignSchedulingService campaignSchedulingService;
    private final MemberService memberService; // Added

    @Autowired
    public CampaignController(CampaignService campaignService,
                              CampaignSchedulingService campaignSchedulingService,
                              MemberService memberService) { // Added
        this.campaignService = campaignService;
        this.campaignSchedulingService = campaignSchedulingService;
        this.memberService = memberService; // Added
    }

    // --- Mapper Methods ---

    // CampaignRoundDto Mapper (Moved here as it's primarily used in Campaign context)
    public static CampaignRoundDto toDto(CampaignRound round) {
        if (round == null) return null;
        CampaignRoundDto dto = new CampaignRoundDto();
        dto.setId(round.getId());
        if (round.getCampaign() != null) {
            dto.setCampaignId(round.getCampaign().getId());
        }
        dto.setRoundNumber(round.getRoundNumber());
        if (round.getEmailTemplate() != null) {
            dto.setEmailTemplateId(round.getEmailTemplate().getId());
            dto.setEmailTemplateName(round.getEmailTemplate().getName()); // Convenience
        }
        dto.setSubjectTemplate(round.getSubjectTemplate());
        dto.setTimeIntervalDays(round.getTimeIntervalDays());
        dto.setScheduledSendTime(round.getScheduledSendTime() != null ? round.getScheduledSendTime().toLocalDateTime() : null);
        dto.setStatus(round.getStatus());
        dto.setCreatedAt(round.getCreatedAt() != null ? round.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(round.getUpdatedAt() != null ? round.getUpdatedAt().toLocalDateTime() : null);
        return dto;
    }

    public static CampaignRound fromCreateRequest(CreateCampaignRoundRequest request) {
        if (request == null) return null;
        CampaignRound round = new CampaignRound();
        // Campaign will be set by service
        EmailTemplate template = new EmailTemplate(); // Temporary, service will fetch actual template
        template.setId(request.getEmailTemplateId());
        round.setEmailTemplate(template);
        round.setSubjectTemplate(request.getSubjectTemplate());
        round.setRoundNumber(request.getRoundNumber());
        round.setTimeIntervalDays(request.getTimeIntervalDays());
        if (request.getScheduledSendTime() != null) {
            round.setScheduledSendTime(Timestamp.valueOf(request.getScheduledSendTime()));
        }
        // Status set by service
        return round;
    }


    public static CampaignDto toDto(Campaign campaign) {
        if (campaign == null) return null;
        CampaignDto dto = new CampaignDto();
        dto.setId(campaign.getId());
        if (campaign.getMember() != null) {
            dto.setMemberId(campaign.getMember().getId());
        }
        dto.setName(campaign.getName());
        dto.setDescription(campaign.getDescription());
        dto.setStatus(campaign.getStatus());
        dto.setCreatedAt(campaign.getCreatedAt() != null ? campaign.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(campaign.getUpdatedAt() != null ? campaign.getUpdatedAt().toLocalDateTime() : null);

        if (campaign.getTargetLists() != null) {
            dto.setTargetLists(campaign.getTargetLists().stream()
                    .map(TargetListController::toDto) // Reuse TargetListController's mapper
                    .collect(Collectors.toList()));
        } else {
            dto.setTargetLists(Collections.emptyList());
        }

        if (campaign.getCampaignRounds() != null) {
            dto.setRounds(campaign.getCampaignRounds().stream()
                    .map(CampaignController::toDto) // Use local CampaignRoundDto mapper
                    .collect(Collectors.toList()));
        } else {
            dto.setRounds(Collections.emptyList());
        }
        return dto;
    }

    public static Campaign fromCreateRequest(CreateCampaignRequest request) {
        if (request == null) return null;
        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        // Member and TargetLists set by service
        return campaign;
    }

    // --- Campaign Endpoints ---

    @PostMapping
    public ResponseEntity<CampaignDto> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        Campaign campaignToCreate = fromCreateRequest(request);
        Campaign createdCampaign = campaignService.createCampaign(campaignToCreate, request.getTargetListIds(), authenticatedMember.getId());
        return new ResponseEntity<>(toDto(createdCampaign), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> getCampaignById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        Campaign campaign = campaignService.getCampaignWithRoundsAndTargets(id, authenticatedMember.getId());
        return ResponseEntity.ok(toDto(campaign));
    }

    @GetMapping("/my-campaigns") // Changed from /by-member/{memberId}
    public ResponseEntity<List<CampaignDto>> getMyCampaigns(
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        List<Campaign> campaigns = campaignService.getCampaignsByMemberId(authenticatedMember.getId());
        List<CampaignDto> dtos = campaigns.stream().map(CampaignController::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignDto> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        Campaign campaignDetails = new Campaign();
        campaignDetails.setName(request.getName());
        campaignDetails.setDescription(request.getDescription());
        if(request.getStatus() != null) campaignDetails.setStatus(request.getStatus());

        Campaign updatedCampaign = campaignService.updateCampaign(id, campaignDetails, request.getTargetListIds(), authenticatedMember.getId());
        return ResponseEntity.ok(toDto(updatedCampaign));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        campaignService.deleteCampaign(id, authenticatedMember.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CampaignDto> updateCampaignStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignStatusRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        Campaign updatedCampaign = campaignService.changeCampaignStatus(id, request.getStatus(), authenticatedMember.getId());
        return ResponseEntity.ok(toDto(updatedCampaign));
    }

    @PostMapping("/{id}/trigger-send")
    public ResponseEntity<Void> triggerCampaignSend(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        campaignSchedulingService.triggerImmediateSend(id, authenticatedMember.getId());
        return ResponseEntity.accepted().build(); // Accepted for processing
    }

    // --- CampaignRound Endpoints (nested under campaign) ---

    @PostMapping("/{campaignId}/rounds")
    public ResponseEntity<CampaignRoundDto> addRoundToCampaign(
            @PathVariable Long campaignId,
            @Valid @RequestBody CreateCampaignRoundRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        CampaignRound roundToCreate = fromCreateRequest(request);
        CampaignRound createdRound = campaignService.addRoundToCampaign(campaignId, roundToCreate, authenticatedMember.getId());
        return new ResponseEntity<>(toDto(createdRound), HttpStatus.CREATED);
    }

    @GetMapping("/{campaignId}/rounds")
    public ResponseEntity<List<CampaignRoundDto>> getCampaignRounds(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        List<CampaignRound> rounds = campaignService.getCampaignRounds(campaignId, authenticatedMember.getId());
        List<CampaignRoundDto> dtos = rounds.stream().map(CampaignController::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{campaignId}/rounds/{roundId}")
    public ResponseEntity<CampaignRoundDto> updateRoundInCampaign(
            @PathVariable Long campaignId,
            @PathVariable Long roundId,
            @Valid @RequestBody UpdateCampaignRoundRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        CampaignRound roundDetails = new CampaignRound();
        if (request.getEmailTemplateId() != null) {
            EmailTemplate et = new EmailTemplate();
            et.setId(request.getEmailTemplateId());
            roundDetails.setEmailTemplate(et);
        }
        roundDetails.setSubjectTemplate(request.getSubjectTemplate());
        roundDetails.setRoundNumber(request.getRoundNumber());
        roundDetails.setTimeIntervalDays(request.getTimeIntervalDays());
        if (request.getScheduledSendTime() != null) {
            roundDetails.setScheduledSendTime(Timestamp.valueOf(request.getScheduledSendTime()));
        }
        if (request.getStatus() != null) {
            roundDetails.setStatus(request.getStatus());
        }

        CampaignRound updatedRound = campaignService.updateRoundInCampaign(campaignId, roundId, roundDetails, memberId);
        return ResponseEntity.ok(toDto(updatedRound));
    }

    @DeleteMapping("/{campaignId}/rounds/{roundId}")
    public ResponseEntity<Void> deleteRoundFromCampaign(
            @PathVariable Long campaignId,
            @PathVariable Long roundId,
            @RequestHeader("X-Member-Id") Long memberId) { // Placeholder
        campaignService.deleteRoundFromCampaign(campaignId, roundId, memberId);
        return ResponseEntity.noContent().build();
    }
}
