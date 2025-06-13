package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.exception.ValidationException;
import com.icegreen.greenmail.marketing.model.entity.*;
import com.icegreen.greenmail.marketing.model.enums.CampaignStatus;
import com.icegreen.greenmail.marketing.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignRoundRepository campaignRoundRepository;
    private final MemberRepository memberRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final TargetListRepository targetListRepository;

    @Autowired
    public CampaignService(CampaignRepository campaignRepository,
                           CampaignRoundRepository campaignRoundRepository,
                           MemberRepository memberRepository,
                           EmailTemplateRepository emailTemplateRepository,
                           TargetListRepository targetListRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignRoundRepository = campaignRoundRepository;
        this.memberRepository = memberRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.targetListRepository = targetListRepository;
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", memberId));
    }

    private Campaign findCampaignByIdAndMemberId(Long campaignId, Long memberId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));
        if (!Objects.equals(campaign.getMember().getId(), memberId)) {
            throw new ResourceNotFoundException("Campaign", "id", campaignId + " (for member " + memberId + ")");
        }
        return campaign;
    }

    private EmailTemplate findEmailTemplateByIdAndMemberId(Long templateId, Long memberId) {
        EmailTemplate template = emailTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", "id", templateId));
        if (!Objects.equals(template.getMember().getId(), memberId)) {
            throw new ResourceNotFoundException("EmailTemplate", "id", templateId + " (for member " + memberId + ")");
        }
        return template;
    }


    @Transactional
    public Campaign createCampaign(Campaign campaign, List<Long> targetListIds, Long memberId) {
        Member member = findMemberById(memberId);
        campaign.setMember(member);

        if (!CollectionUtils.isEmpty(targetListIds)) {
            Set<TargetList> associatedTargetLists = new HashSet<>();
            for (Long listId : targetListIds) {
                TargetList tl = targetListRepository.findById(listId)
                        .orElseThrow(() -> new ResourceNotFoundException("TargetList", "id", listId));
                if (!Objects.equals(tl.getMember().getId(), memberId)) {
                    throw new ValidationException("TargetList with id " + listId + " does not belong to the member.");
                }
                associatedTargetLists.add(tl);
            }
            campaign.setTargetLists(associatedTargetLists);
        }
        campaign.setStatus(CampaignStatus.DRAFT); // Ensure default status
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", id));
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignByIdAndMemberId(Long id, Long memberId) {
        return findCampaignByIdAndMemberId(id, memberId);
    }


    @Transactional(readOnly = true)
    public List<Campaign> getCampaignsByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", "id", memberId);
        }
        return campaignRepository.findByMemberId(memberId);
    }

    @Transactional
    public Campaign updateCampaign(Long id, Campaign campaignDetails, List<Long> targetListIds, Long memberId) {
        Campaign existingCampaign = findCampaignByIdAndMemberId(id, memberId);

        if (StringUtils.hasText(campaignDetails.getName())) {
            existingCampaign.setName(campaignDetails.getName());
        }
        if (StringUtils.hasText(campaignDetails.getDescription())) {
            existingCampaign.setDescription(campaignDetails.getDescription());
        }
        // Status update should likely go through changeCampaignStatus for specific logic
        if (campaignDetails.getStatus() != null && existingCampaign.getStatus() != campaignDetails.getStatus()
             && existingCampaign.getStatus() == CampaignStatus.DRAFT) { // Only allow direct status change if draft
            existingCampaign.setStatus(campaignDetails.getStatus());
        }


        if (targetListIds != null) { // Allow empty list to clear associations
            Set<TargetList> associatedTargetLists = new HashSet<>();
            for (Long listId : targetListIds) {
                TargetList tl = targetListRepository.findById(listId)
                        .orElseThrow(() -> new ResourceNotFoundException("TargetList", "id", listId));
                if (!Objects.equals(tl.getMember().getId(), memberId)) {
                    throw new ValidationException("TargetList with id " + listId + " does not belong to the member.");
                }
                associatedTargetLists.add(tl);
            }
            existingCampaign.setTargetLists(associatedTargetLists);
        }

        return campaignRepository.save(existingCampaign);
    }

    @Transactional
    public void deleteCampaign(Long id, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(id, memberId);
        // Cascade should handle CampaignRounds and CampaignTargetLists
        campaignRepository.delete(campaign);
    }

    @Transactional
    public CampaignRound addRoundToCampaign(Long campaignId, CampaignRound round, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(campaignId, memberId);

        // Verify EmailTemplate exists and belongs to member
        EmailTemplate template = findEmailTemplateByIdAndMemberId(round.getEmailTemplate().getId(), memberId);
        round.setEmailTemplate(template);

        // Check for round_number uniqueness within the campaign
        campaignRoundRepository.findByCampaignIdAndRoundNumber(campaignId, round.getRoundNumber())
            .ifPresent(r -> {
                throw new ValidationException("Round number " + round.getRoundNumber() + " already exists for this campaign.");
            });

        round.setCampaign(campaign);
        round.setStatus(CampaignRoundStatus.PENDING); // Default status
        return campaignRoundRepository.save(round);
    }

    @Transactional
    public CampaignRound updateRoundInCampaign(Long campaignId, Long roundId, CampaignRound roundDetails, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(campaignId, memberId);
        CampaignRound existingRound = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignRound", "id", roundId));

        if (!Objects.equals(existingRound.getCampaign().getId(), campaign.getId())) {
            throw new ValidationException("Round does not belong to the specified campaign.");
        }

        if (roundDetails.getEmailTemplate() != null && roundDetails.getEmailTemplate().getId() != null) {
             if (existingRound.getEmailTemplate() == null || !Objects.equals(existingRound.getEmailTemplate().getId(), roundDetails.getEmailTemplate().getId())) {
                EmailTemplate template = findEmailTemplateByIdAndMemberId(roundDetails.getEmailTemplate().getId(), memberId);
                existingRound.setEmailTemplate(template);
            }
        }
        if (StringUtils.hasText(roundDetails.getSubjectTemplate())) {
            existingRound.setSubjectTemplate(roundDetails.getSubjectTemplate());
        }
        if (roundDetails.getRoundNumber() != null && !Objects.equals(existingRound.getRoundNumber(), roundDetails.getRoundNumber())) {
            // Check uniqueness if round number changes
            campaignRoundRepository.findByCampaignIdAndRoundNumber(campaignId, roundDetails.getRoundNumber())
                .ifPresent(r -> {
                    if(!Objects.equals(r.getId(), existingRound.getId())) { // ensure it's not the same round
                         throw new ValidationException("Round number " + roundDetails.getRoundNumber() + " already exists for this campaign.");
                    }
                });
            existingRound.setRoundNumber(roundDetails.getRoundNumber());
        }
        if (roundDetails.getTimeIntervalDays() != null) { // Allow setting to null
            existingRound.setTimeIntervalDays(roundDetails.getTimeIntervalDays());
        }
        if (roundDetails.getScheduledSendTime() != null) { // Allow setting to null
            existingRound.setScheduledSendTime(roundDetails.getScheduledSendTime());
        }
        // Status updates should ideally have their own methods for more complex logic
        if (roundDetails.getStatus() != null && existingRound.getStatus() != roundDetails.getStatus()
            && existingRound.getStatus() == CampaignRoundStatus.PENDING) { // Only from PENDING
            existingRound.setStatus(roundDetails.getStatus());
        }

        return campaignRoundRepository.save(existingRound);
    }

    @Transactional
    public void deleteRoundFromCampaign(Long campaignId, Long roundId, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(campaignId, memberId);
        CampaignRound round = campaignRoundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignRound", "id", roundId));

        if (!Objects.equals(round.getCampaign().getId(), campaign.getId())) {
            throw new ValidationException("Round does not belong to the specified campaign.");
        }
        campaignRoundRepository.delete(round);
    }

    @Transactional(readOnly = true)
    public List<CampaignRound> getCampaignRounds(Long campaignId, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(campaignId, memberId);
        return campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());
    }

    @Transactional
    public Campaign changeCampaignStatus(Long campaignId, CampaignStatus newStatus, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(campaignId, memberId);

        // Add business logic for status transitions here.
        // For example, can only move from DRAFT to ACTIVE if certain conditions are met.
        // If moving to ACTIVE, schedule first round, etc.
        // For now, simple status update.
        if (campaign.getStatus() == CampaignStatus.COMPLETED || campaign.getStatus() == CampaignStatus.ARCHIVED) {
            if (newStatus != CampaignStatus.ARCHIVED && newStatus != CampaignStatus.COMPLETED) { // simple rule: can't re-activate easily
                 throw new ValidationException("Cannot change status from " + campaign.getStatus() + " to " + newStatus);
            }
        }
        campaign.setStatus(newStatus);
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignWithRoundsAndTargets(Long campaignId, Long memberId) {
        Campaign campaign = findCampaignByIdAndMemberId(campaignId, memberId);
        // Eagerly fetch rounds and target lists.
        // This can be done via Hibernate.initialize() or by ensuring the transaction is open
        // when accessing these lazy-loaded collections in the caller (e.g. controller).
        // Or, define a query with JOIN FETCH in the repository.
        // For simplicity, let's assume the transaction keeps them accessible.
        // Accessing them here will trigger loading if not already loaded.
        campaign.getCampaignRounds().size(); // Trigger loading
        campaign.getTargetLists().size(); // Trigger loading
        return campaign;
    }
}
