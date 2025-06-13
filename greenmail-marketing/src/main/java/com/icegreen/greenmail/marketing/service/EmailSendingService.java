package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.model.entity.*;
import com.icegreen.greenmail.marketing.model.enums.CampaignRoundStatus;
import com.icegreen.greenmail.marketing.model.enums.SentEmailStatus;
import com.icegreen.greenmail.marketing.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailSendingService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSendingService.class);

    // Pattern for basic placeholders like {{firstName}}, {{lastName}}, {{email}}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final JavaMailSender globalMailSender; // Can be used if a default configuration is primary
    private final EmailAccountRepository emailAccountRepository;
    private final SentEmailLogRepository sentEmailLogRepository;
    private final CampaignRoundRepository campaignRoundRepository;
    private final CampaignRepository campaignRepository; // To get campaign owner for email account.

    @Autowired
    public EmailSendingService(JavaMailSender globalMailSender,
                               EmailAccountRepository emailAccountRepository,
                               SentEmailLogRepository sentEmailLogRepository,
                               CampaignRoundRepository campaignRoundRepository,
                               CampaignRepository campaignRepository) {
        this.globalMailSender = globalMailSender;
        this.emailAccountRepository = emailAccountRepository;
        this.sentEmailLogRepository = sentEmailLogRepository;
        this.campaignRoundRepository = campaignRoundRepository;
        this.campaignRepository = campaignRepository;
    }

    private JavaMailSender getConfiguredMailSender(EmailAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getSmtpHost());
        sender.setPort(account.getSmtpPort());
        sender.setUsername(account.getSmtpUsername());
        sender.setPassword(account.getSmtpPassword()); // In real app, this should be decrypted

        Properties props = sender.getJavaMailProperties();
        if ("smtps".equalsIgnoreCase(account.getSmtpProtocol())) {
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true"); // Often needed for SMTPS, or direct SSL on specific port
            props.put("mail.smtp.ssl.enable", "true");
        } else { // Assuming plain SMTP, could add more specific TLS handling
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true"); // Assuming auth is typically needed
            // Check if STARTTLS is needed; typically it is for non-SSL ports
            // For simplicity, this example assumes explicit SMTPS or basic SMTP
            // props.put("mail.smtp.starttls.enable", "true");
        }
        return sender;
    }

    private String personalizeContent(String content, TargetListContact contact) {
        if (content == null) return null;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = "";
            switch (placeholder.toLowerCase()) {
                case "firstname":
                    replacement = contact.getFirstName() != null ? contact.getFirstName() : "";
                    break;
                case "lastname":
                    replacement = contact.getLastName() != null ? contact.getLastName() : "";
                    break;
                case "email":
                    replacement = contact.getEmailAddress();
                    break;
                // Add more placeholders for custom_fields if needed
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW) // New transaction for each async execution
    public void sendEmailForCampaignRound(Long campaignRoundId) {
        logger.info("Starting to send emails for campaign round ID: {}", campaignRoundId);
        CampaignRound round = campaignRoundRepository.findById(campaignRoundId)
                .orElseThrow(() -> {
                    logger.error("CampaignRound not found with ID: {}", campaignRoundId);
                    return new ResourceNotFoundException("CampaignRound", "id", campaignRoundId);
                });

        Campaign campaign = round.getCampaign();
        EmailTemplate template = round.getEmailTemplate();
        Member campaignOwner = campaign.getMember();

        // Determine EmailAccount for sending. For now, use the first one for the member.
        // A more robust strategy might be needed (e.g., specific account per campaign).
        List<EmailAccount> memberAccounts = emailAccountRepository.findByMemberId(campaignOwner.getId());
        if (memberAccounts.isEmpty()) {
            logger.error("No email accounts found for member ID: {}. Cannot send campaign round ID: {}", campaignOwner.getId(), campaignRoundId);
            round.setStatus(CampaignRoundStatus.FAILED);
            round.setUpdatedAt(Timestamp.from(Instant.now())); // Manually update timestamp for status change
            campaignRoundRepository.save(round);
            return;
        }
        EmailAccount sendingAccount = memberAccounts.get(0); // Using the first account
        JavaMailSender mailSender = getConfiguredMailSender(sendingAccount);

        int totalContacts = 0;
        int successfulSends = 0;
        int failedSends = 0;

        for (TargetList targetList : campaign.getTargetLists()) {
            List<TargetListContact> contacts = targetList.getContacts().stream()
                .filter(TargetListContact::isSubscribed) // Only subscribed contacts
                .collect(java.util.stream.Collectors.toList());

            totalContacts += contacts.size();

            for (TargetListContact contact : contacts) {
                SentEmailLog logEntry = new SentEmailLog();
                logEntry.setCampaignRound(round);
                logEntry.setContact(contact);
                logEntry.setEmailAccount(sendingAccount);
                logEntry.setStatus(SentEmailStatus.PREPARING);
                // createdAt and updatedAt will be set by DB
                sentEmailLogRepository.save(logEntry); // Save initial log

                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8"); // true for multipart

                    helper.setFrom(sendingAccount.getEmailAddress()); // Or a "friendly name" <email@example.com>
                    helper.setTo(contact.getEmailAddress());

                    String subject = personalizeContent(round.getSubjectTemplate(), contact);
                    helper.setSubject(subject);

                    String htmlContent = personalizeContent(template.getContentHtml(), contact);
                    String textContent = personalizeContent(template.getContentText(), contact);

                    if (htmlContent != null && textContent != null) {
                        helper.setText(textContent, htmlContent);
                    } else if (htmlContent != null) {
                        helper.setText(htmlContent, true); // true means HTML
                    } else if (textContent != null) {
                        helper.setText(textContent, false); // false means plain text
                    } else {
                        logger.warn("Email template for round {} has no content for contact {}", campaignRoundId, contact.getId());
                        logEntry.setStatus(SentEmailStatus.FAILED);
                        logEntry.setErrorMessage("Template has no content.");
                        failedSends++;
                        sentEmailLogRepository.save(logEntry);
                        continue;
                    }

                    // Add custom headers if needed, e.g., List-Unsubscribe

                    mailSender.send(mimeMessage);

                    logEntry.setStatus(SentEmailStatus.SENT);
                    logEntry.setSentAt(Timestamp.from(Instant.now()));
                    String[] messageIdHeader = mimeMessage.getHeader("Message-ID");
                    if (messageIdHeader != null && messageIdHeader.length > 0) {
                        logEntry.setMessageIdHeader(messageIdHeader[0]);
                    }
                    successfulSends++;
                } catch (MessagingException | org.springframework.mail.MailException e) {
                    logger.error("Failed to send email to contact ID: {} for campaign round ID: {}", contact.getId(), campaignRoundId, e);
                    logEntry.setStatus(SentEmailStatus.FAILED);
                    logEntry.setErrorMessage(e.getMessage());
                    failedSends++;
                } finally {
                    logEntry.setUpdatedAt(Timestamp.from(Instant.now())); // Manually update timestamp
                    sentEmailLogRepository.save(logEntry);
                }
            }
        }

        if (failedSends == 0 && totalContacts > 0) {
            round.setStatus(CampaignRoundStatus.SENT);
        } else if (successfulSends > 0 && failedSends > 0) {
            // round.setStatus(CampaignRoundStatus.PARTIALLY_FAILED); // Need this status
            logger.warn("Campaign round {} completed with some failures.", campaignRoundId);
            // For now, use SENT if any success, or FAILED if all failed and there were contacts
             round.setStatus(CampaignRoundStatus.SENT); // Or a more specific status
        } else if (failedSends > 0 && successfulSends == 0 && totalContacts > 0) {
            round.setStatus(CampaignRoundStatus.FAILED);
        } else if (totalContacts == 0) {
            logger.info("No contacts to send for campaign round ID: {}", campaignRoundId);
            round.setStatus(CampaignRoundStatus.SKIPPED); // Or SENT if "nothing to send" is considered "sent"
        } else {
             round.setStatus(CampaignRoundStatus.SENT); // Default to SENT if no errors and contacts processed
        }
        round.setUpdatedAt(Timestamp.from(Instant.now()));
        campaignRoundRepository.save(round);
        logger.info("Finished sending emails for campaign round ID: {}. Total: {}, Successful: {}, Failed: {}",
            campaignRoundId, totalContacts, successfulSends, failedSends);
    }

    @Transactional
    public void resendFailedEmailsForRound(Long campaignRoundId) {
        // Stub: Find SentEmailLog entries with FAILED status for this round
        // and attempt to resend them. Be careful about retry limits and reasons for failure.
        logger.info("ResendFailedEmailsForRound called for round ID: {} (Not implemented yet)", campaignRoundId);
         List<SentEmailLog> failedLogs = sentEmailLogRepository.findByCampaignRoundId(campaignRoundId)
            .stream().filter(log -> log.getStatus() == SentEmailStatus.FAILED)
            .collect(java.util.stream.Collectors.toList());

        if(failedLogs.isEmpty()) {
            logger.info("No failed emails to resend for campaign round ID: {}", campaignRoundId);
            return;
        }
        // This would require similar logic to sendEmailForCampaignRound but targeted,
        // and potentially updating the same log entries or creating new ones.
        // For now, just logging.
        logger.warn("Resending {} failed emails for round {} is not fully implemented.", failedLogs.size(), campaignRoundId);

    }

    // Stubs for advanced tracking
    public void processBounce(String originalMessageId, String bounceReason) {
        logger.info("Processing bounce for message ID: {} with reason: {} (Not implemented yet)", originalMessageId, bounceReason);
        // Find SentEmailLog by messageIdHeader, update status to BOUNCED, update contact if needed.
    }

    public void processOpening(String originalMessageId) {
        logger.info("Processing open for message ID: {} (Not implemented yet)", originalMessageId);
        // Find SentEmailLog by messageIdHeader, update status to OPENED.
    }

    public void processClick(String originalMessageId, String linkUrl) {
        logger.info("Processing click for message ID: {} on URL: {} (Not implemented yet)", originalMessageId, linkUrl);
        // Find SentEmailLog by messageIdHeader, update status to CLICKED, maybe log the URL.
    }
}
