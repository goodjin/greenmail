package com.icegreen.greenmail.marketing.controller;

import com.icegreen.greenmail.marketing.dto.SentEmailLogDto;
import com.icegreen.greenmail.marketing.model.entity.SentEmailLog;
import com.icegreen.greenmail.marketing.model.enums.SentEmailStatus;
import com.icegreen.greenmail.marketing.model.entity.Campaign;
import com.icegreen.greenmail.marketing.model.entity.CampaignRound;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.model.entity.TargetListContact;
import com.icegreen.greenmail.marketing.repository.CampaignRoundRepository;
import com.icegreen.greenmail.marketing.repository.SentEmailLogRepository;
import com.icegreen.greenmail.marketing.repository.TargetListContactRepository;
import com.icegreen.greenmail.marketing.service.CampaignService;
import com.icegreen.greenmail.marketing.service.MemberService;
import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // For 403 Forbidden
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // For principal
import org.springframework.security.core.userdetails.UserDetails; // For principal
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/email-logs")
public class EmailLogController {

    private final SentEmailLogRepository sentEmailLogRepository;
    private final MemberService memberService;
    private final CampaignService campaignService;
    private final CampaignRoundRepository campaignRoundRepository; // To fetch round for ownership check
    private final TargetListContactRepository targetListContactRepository; // To fetch contact for ownership check


    @Autowired
    public EmailLogController(SentEmailLogRepository sentEmailLogRepository,
                              MemberService memberService,
                              CampaignService campaignService,
                              CampaignRoundRepository campaignRoundRepository,
                              TargetListContactRepository targetListContactRepository) {
        this.sentEmailLogRepository = sentEmailLogRepository;
        this.memberService = memberService;
        this.campaignService = campaignService;
        this.campaignRoundRepository = campaignRoundRepository;
        this.targetListContactRepository = targetListContactRepository;
    }

    // --- Mapper Method ---
    public static SentEmailLogDto toDto(SentEmailLog log) {
        if (log == null) return null;
        SentEmailLogDto dto = new SentEmailLogDto();
        dto.setId(log.getId());
        if (log.getCampaignRound() != null) {
            dto.setCampaignRoundId(log.getCampaignRound().getId());
        }
        if (log.getContact() != null) {
            dto.setContactId(log.getContact().getId());
            dto.setContactEmail(log.getContact().getEmailAddress()); // Denormalized
        }
        if (log.getEmailAccount() != null) {
            dto.setEmailAccountId(log.getEmailAccount().getId());
            dto.setSendingEmailAddress(log.getEmailAccount().getEmailAddress()); // Denormalized
        }
        dto.setSentAt(log.getSentAt() != null ? log.getSentAt().toLocalDateTime() : null);
        dto.setStatus(log.getStatus());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setMessageIdHeader(log.getMessageIdHeader());
        dto.setCreatedAt(log.getCreatedAt() != null ? log.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(log.getUpdatedAt() != null ? log.getUpdatedAt().toLocalDateTime() : null);
        return dto;
    }

    // --- Controller Methods ---

    @GetMapping("/by-round/{roundId}")
    public ResponseEntity<List<SentEmailLogDto>> getLogsByRound(
            @PathVariable Long roundId,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignRound", "id", roundId));
        // Verify ownership of the campaign this round belongs to
        if (!Objects.equals(round.getCampaign().getMember().getId(), authenticatedMember.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<SentEmailLog> logs = sentEmailLogRepository.findByCampaignRoundId(roundId);
        List<SentEmailLogDto> dtos = logs.stream().map(EmailLogController::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/by-contact/{contactId}")
    public ResponseEntity<List<SentEmailLogDto>> getLogsByContact(
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        TargetListContact contact = targetListContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("TargetListContact", "id", contactId));
        // Verify ownership of the target list this contact belongs to
        if (!Objects.equals(contact.getTargetList().getMember().getId(), authenticatedMember.getId())) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<SentEmailLog> logs = sentEmailLogRepository.findByContactId(contactId);
        List<SentEmailLogDto> dtos = logs.stream().map(EmailLogController::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<SentEmailLogDto>> getLogsByStatus(
            @RequestParam("status") SentEmailStatus status,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        // This is less direct. Fetch all logs by status, then filter by member ownership of the campaign.
        // This could be inefficient. A custom repository query would be better.
        List<SentEmailLog> allLogsByStatus = sentEmailLogRepository.findByStatus(status);
        List<SentEmailLogDto> dtos = allLogsByStatus.stream()
                .filter(log -> log.getCampaignRound() != null &&
                               log.getCampaignRound().getCampaign() != null &&
                               Objects.equals(log.getCampaignRound().getCampaign().getMember().getId(), authenticatedMember.getId()))
                .map(EmailLogController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{logId}")
    public ResponseEntity<SentEmailLogDto> getLogById(
            @PathVariable Long logId,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        SentEmailLog log = sentEmailLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("SentEmailLog", "id", logId));

        // Verify ownership: SentEmailLog -> CampaignRound -> Campaign -> Member
        if (log.getCampaignRound() == null || log.getCampaignRound().getCampaign() == null ||
            !Objects.equals(log.getCampaignRound().getCampaign().getMember().getId(), authenticatedMember.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toDto(log));
    }
}
