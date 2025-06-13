package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.model.entity.Campaign;
import com.icegreen.greenmail.marketing.model.entity.CampaignRound;
import com.icegreen.greenmail.marketing.model.enums.CampaignRoundStatus;
import com.icegreen.greenmail.marketing.model.enums.CampaignStatus;
import com.icegreen.greenmail.marketing.repository.CampaignRepository;
import com.icegreen.greenmail.marketing.repository.CampaignRoundRepository;
import com.icegreen.greenmail.marketing.repository.SentEmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CampaignSchedulingService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignSchedulingService.class);

    private final CampaignRepository campaignRepository;
    private final CampaignRoundRepository campaignRoundRepository;
    private final EmailSendingService emailSendingService;
    private final SentEmailLogRepository sentEmailLogRepository; // To check previous round's sent status

    @Autowired
    public CampaignSchedulingService(CampaignRepository campaignRepository,
                                     CampaignRoundRepository campaignRoundRepository,
                                     EmailSendingService emailSendingService,
                                     SentEmailLogRepository sentEmailLogRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignRoundRepository = campaignRoundRepository;
        this.emailSendingService = emailSendingService;
        this.sentEmailLogRepository = sentEmailLogRepository;
    }

    @Scheduled(cron = "0 * * * * ?") // Run every minute for testing; "0 0 * * * ?" for hourly
    @Transactional
    public void scheduleCampaigns() {
        logger.info("Running campaign scheduling task...");
        List<Campaign> activeCampaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE);

        for (Campaign campaign : activeCampaigns) {
            logger.debug("Checking campaign ID: {}", campaign.getId());
            List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());

            CampaignRound lastSuccessfullySentRound = null;
            // Find the last round that was actually SENT (or PARTIALLY_FAILED if we add that status)
            for (int i = rounds.size() - 1; i >= 0; i--) {
                CampaignRound r = rounds.get(i);
                if (r.getStatus() == CampaignRoundStatus.SENT) {
                     // Check if all emails were sent for this round to truly consider it "sent" for timing next round
                    long totalEmailsForRound = sentEmailLogRepository.countByCampaignRoundId(r.getId());
                    long successfulEmailsForRound = sentEmailLogRepository.countByCampaignRoundIdAndStatus(r.getId(), com.icegreen.greenmail.marketing.model.enums.SentEmailStatus.SENT);
                    if (totalEmailsForRound > 0 && totalEmailsForRound == successfulEmailsForRound) {
                        lastSuccessfullySentRound = r;
                        break;
                    } else if (totalEmailsForRound == 0 && r.getStatus() == CampaignRoundStatus.SKIPPED) { // A skipped round can be a base for next
                        lastSuccessfullySentRound = r; // Treat skipped (no contacts) as a valid predecessor for timing
                        break;
                    }
                } else if (r.getStatus() == CampaignRoundStatus.SKIPPED) { // A skipped round can be a base for next
                     lastSuccessfullySentRound = r;
                     break;
                }
            }


            for (CampaignRound round : rounds) {
                if (round.getStatus() == CampaignRoundStatus.PENDING || round.getStatus() == CampaignRoundStatus.CONFIGURING) {
                    LocalDateTime scheduledTime = null;

                    if (round.getScheduledSendTime() != null) {
                        scheduledTime = round.getScheduledSendTime().toLocalDateTime();
                    } else if (round.getTimeIntervalDays() != null) {
                        if (round.getRoundNumber() == 1) {
                            // Schedule based on campaign's created_at (or a specific start_date if added to Campaign)
                            // For simplicity, using campaign's created_at.
                            LocalDateTime campaignStartTime = campaign.getCreatedAt().toLocalDateTime();
                            scheduledTime = campaignStartTime.plusDays(round.getTimeIntervalDays());
                        } else {
                            // Find previous round (must exist if this is not round 1)
                            final int prevRoundNumber = round.getRoundNumber() - 1;
                            Optional<CampaignRound> prevRoundOpt = rounds.stream()
                                    .filter(r -> r.getRoundNumber().equals(prevRoundNumber))
                                    .findFirst();

                            if (prevRoundOpt.isPresent() &&
                                (prevRoundOpt.get().getStatus() == CampaignRoundStatus.SENT || prevRoundOpt.get().getStatus() == CampaignRoundStatus.SKIPPED)) {
                                // Use the `updated_at` of the previous round as its completion time
                                LocalDateTime prevRoundSentTime = prevRoundOpt.get().getUpdatedAt().toLocalDateTime();
                                scheduledTime = prevRoundSentTime.plusDays(round.getTimeIntervalDays());
                            } else if (lastSuccessfullySentRound != null && lastSuccessfullySentRound.getRoundNumber().equals(prevRoundNumber)) {
                                // Fallback to last successfully sent round if direct previous not suitable
                                LocalDateTime prevRoundSentTime = lastSuccessfullySentRound.getUpdatedAt().toLocalDateTime();
                                scheduledTime = prevRoundSentTime.plusDays(round.getTimeIntervalDays());
                            } else {
                                logger.debug("Previous round for campaign ID {}, round {} not successfully sent yet. Skipping round {}.",
                                        campaign.getId(), prevRoundNumber, round.getRoundNumber());
                                continue; // Skip this round for now
                            }
                        }
                    }

                    if (scheduledTime != null && scheduledTime.isBefore(LocalDateTime.now())) {
                        logger.info("Calculated scheduled time {} is in the past for round ID {}. Triggering send.", scheduledTime, round.getId());
                        // Update status and call async email sending
                        round.setStatus(CampaignRoundStatus.SENDING);
                        round.setScheduledSendTime(Timestamp.valueOf(scheduledTime)); // Store the calculated/actual schedule time
                        round.setUpdatedAt(Timestamp.from(Instant.now()));
                        campaignRoundRepository.save(round);
                        emailSendingService.sendEmailForCampaignRound(round.getId());
                        // After triggering one round for a campaign, break to process next campaign.
                        // This prevents sending multiple rounds of the same campaign in one scheduler run.
                        break;
                    } else if (scheduledTime != null) {
                        logger.debug("Round ID {} for campaign {} is scheduled for {}. Not yet time.", round.getId(), campaign.getId(), scheduledTime);
                         // Persist calculated scheduled time if it was based on interval and not yet set
                        if (round.getScheduledSendTime() == null && round.getTimeIntervalDays() != null) {
                            round.setScheduledSendTime(Timestamp.valueOf(scheduledTime));
                            campaignRoundRepository.save(round);
                        }
                    }
                }
            }
        }
        logger.info("Campaign scheduling task finished.");
    }

    @Transactional
    public void triggerImmediateSend(Long campaignId, Long memberId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));

        if (!campaign.getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("Campaign", "id", campaignId + " (for member " + memberId + ")");
        }

        if (campaign.getStatus() != CampaignStatus.ACTIVE && campaign.getStatus() != CampaignStatus.DRAFT) {
            logger.warn("Campaign ID: {} is not ACTIVE or DRAFT. Current status: {}. Cannot trigger immediate send.", campaignId, campaign.getStatus());
            // Optionally throw ValidationException
            return;
        }

        if(campaign.getStatus() == CampaignStatus.DRAFT) {
            campaign.setStatus(CampaignStatus.ACTIVE); // Activate if it was in draft
        }

        List<CampaignRound> rounds = campaignRoundRepository.findByCampaignIdOrderByRoundNumberAsc(campaign.getId());
        CampaignRound nextRoundToSend = null;

        // Find the first PENDING or CONFIGURING round
        for (CampaignRound round : rounds) {
            if (round.getStatus() == CampaignRoundStatus.PENDING || round.getStatus() == CampaignRoundStatus.CONFIGURING) {
                nextRoundToSend = round;
                break;
            }
        }

        // If no pending/configuring, find the first round overall if campaign was just activated from draft
        if (nextRoundToSend == null && !rounds.isEmpty()) {
            nextRoundToSend = rounds.get(0);
            // Reset its status if it was e.g. SENT/FAILED from a previous run and we want to re-trigger
            // This logic depends on desired re-trigger behavior. For now, only PENDING/CONFIGURING are auto-picked.
            // If we want to allow re-triggering a specific round, that's a different function.
        }


        if (nextRoundToSend != null) {
            logger.info("Triggering immediate send for campaign ID: {}, round ID: {}", campaignId, nextRoundToSend.getId());
            nextRoundToSend.setScheduledSendTime(Timestamp.from(Instant.now().minusSeconds(1))); // Set to 1 sec ago to ensure scheduler picks it up
            nextRoundToSend.setStatus(CampaignRoundStatus.CONFIGURING); // Or PENDING, scheduler will move to SENDING
            nextRoundToSend.setUpdatedAt(Timestamp.from(Instant.now()));
            campaignRoundRepository.save(nextRoundToSend);

            // Optionally, directly call the scheduler or email sending service if truly "immediate" is needed
            // For now, setting schedule time and relying on next scheduler run (within a minute)
            // emailSendingService.sendEmailForCampaignRound(nextRoundToSend.getId()); // This would bypass scheduler logic for timing
        } else {
            logger.warn("No pending rounds to trigger for immediate send for campaign ID: {}", campaignId);
            // Optionally, if all rounds are SENT, mark campaign as COMPLETED
            boolean allSentOrSkipped = rounds.stream().allMatch(r -> r.getStatus() == CampaignRoundStatus.SENT || r.getStatus() == CampaignRoundStatus.SKIPPED);
            if(!rounds.isEmpty() && allSentOrSkipped) {
                campaign.setStatus(CampaignStatus.COMPLETED);
                logger.info("Campaign ID {} marked as COMPLETED as all rounds are sent/skipped.", campaignId);
            }
        }
        campaign.setUpdatedAt(Timestamp.from(Instant.now()));
        campaignRepository.save(campaign);
    }
}
