package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.model.entity.EmailAccount;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.repository.EmailAccountRepository;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class EmailAccountService {

    private final EmailAccountRepository emailAccountRepository;
    private final MemberRepository memberRepository; // To fetch Member for association

    @Autowired
    public EmailAccountService(EmailAccountRepository emailAccountRepository, MemberRepository memberRepository) {
        this.emailAccountRepository = emailAccountRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public EmailAccount createEmailAccount(EmailAccount emailAccount, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", memberId));

        emailAccount.setMember(member);

        // Future: Encrypt smtp_password before saving
        // For now, password stored as is (as per schema definition comment)
        // String encryptedPassword = passwordEncoder.encode(emailAccount.getSmtpPassword());
        // emailAccount.setSmtpPassword(encryptedPassword);

        return emailAccountRepository.save(emailAccount);
    }

    @Transactional(readOnly = true)
    public EmailAccount getEmailAccountById(Long id) {
        return emailAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmailAccount", "id", id));
    }

    @Transactional(readOnly = true)
    public List<EmailAccount> getEmailAccountsByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", "id", memberId);
        }
        return emailAccountRepository.findByMemberId(memberId);
    }

    @Transactional
    public EmailAccount updateEmailAccount(Long id, EmailAccount emailAccountDetails) {
        EmailAccount existingAccount = getEmailAccountById(id); // Handles not found

        // Update fields if provided in details
        if (StringUtils.hasText(emailAccountDetails.getEmailAddress())) {
            existingAccount.setEmailAddress(emailAccountDetails.getEmailAddress());
        }
        if (StringUtils.hasText(emailAccountDetails.getSmtpHost())) {
            existingAccount.setSmtpHost(emailAccountDetails.getSmtpHost());
        }
        if (emailAccountDetails.getSmtpPort() != null) {
            existingAccount.setSmtpPort(emailAccountDetails.getSmtpPort());
        }
        if (StringUtils.hasText(emailAccountDetails.getSmtpUsername())) {
            existingAccount.setSmtpUsername(emailAccountDetails.getSmtpUsername());
        }
        if (StringUtils.hasText(emailAccountDetails.getSmtpPassword())) {
            // Future: Encrypt password if it's being changed
            // String encryptedPassword = passwordEncoder.encode(emailAccountDetails.getSmtpPassword());
            // existingAccount.setSmtpPassword(encryptedPassword);
            existingAccount.setSmtpPassword(emailAccountDetails.getSmtpPassword());
        }
        if (StringUtils.hasText(emailAccountDetails.getSmtpProtocol())) {
            existingAccount.setSmtpProtocol(emailAccountDetails.getSmtpProtocol());
        }

        return emailAccountRepository.save(existingAccount);
    }

    @Transactional
    public void deleteEmailAccount(Long id) {
        EmailAccount emailAccount = getEmailAccountById(id); // Handles not found
        emailAccountRepository.delete(emailAccount);
    }
}
