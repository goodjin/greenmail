package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.exception.ValidationException; // Assuming you might need it
import com.icegreen.greenmail.marketing.model.entity.EmailTemplate;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.repository.EmailTemplateRepository;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.util.List;
import java.util.Objects;

@Service
public class TemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public TemplateService(EmailTemplateRepository emailTemplateRepository, MemberRepository memberRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public EmailTemplate createTemplate(EmailTemplate template, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", memberId));
        template.setMember(member);
        return emailTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public EmailTemplate getTemplateById(Long id) {
        return emailTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmailTemplate", "id", id));
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> getTemplatesByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", "id", memberId);
        }
        return emailTemplateRepository.findByMemberId(memberId);
    }

    @Transactional
    public EmailTemplate updateTemplate(Long id, EmailTemplate templateDetails, Long memberId) {
        EmailTemplate existingTemplate = getTemplateById(id);
        if (!Objects.equals(existingTemplate.getMember().getId(), memberId)) {
            throw new ResourceNotFoundException("EmailTemplate", "id", id); // Or a specific access denied exception
        }

        if (StringUtils.hasText(templateDetails.getName())) {
            existingTemplate.setName(templateDetails.getName());
        }
        // HTML content can be nullified
        existingTemplate.setContentHtml(templateDetails.getContentHtml());
        // Text content can be nullified
        existingTemplate.setContentText(templateDetails.getContentText());

        return emailTemplateRepository.save(existingTemplate);
    }

    @Transactional
    public void deleteTemplate(Long id, Long memberId) {
        EmailTemplate template = getTemplateById(id);
        if (!Objects.equals(template.getMember().getId(), memberId)) {
            // Or a specific access denied/forbidden exception
            throw new ResourceNotFoundException("EmailTemplate", "id", id + " (for member " + memberId + ")");
        }

        // Consider checking if template is in use by active/pending campaign rounds.
        // For now, simple delete. Foreign key constraint on campaign_rounds (ON DELETE RESTRICT)
        // for email_template_id will prevent deletion if it's in use.
        // If that constraint is too strict, this service method would be the place
        // to add checks for active campaigns.

        emailTemplateRepository.delete(template);
    }
}
