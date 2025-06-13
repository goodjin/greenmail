package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.exception.ValidationException;
import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.repository.EnterpriseRepository;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final EnterpriseRepository enterpriseRepository; // To fetch Enterprise for association
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository,
                         EnterpriseRepository enterpriseRepository,
                         PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member registerMember(Member member, Long enterpriseId) {
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            throw new ValidationException("Email '" + member.getEmail() + "' is already registered.");
        }

        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enterprise", "id", enterpriseId));

        member.setEnterprise(enterprise);
        member.setPasswordHash(passwordEncoder.encode(member.getPasswordHash())); // Assume raw password was temporarily set in passwordHash field

        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", id));
    }

    @Transactional(readOnly = true)
    public Optional<Member> getMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Member getMemberByEmailOrThrow(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "email", email));
    }


    @Transactional(readOnly = true)
    public List<Member> getMembersByEnterpriseId(Long enterpriseId) {
        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new ResourceNotFoundException("Enterprise", "id", enterpriseId);
        }
        // This assumes Member entity has a field that directly stores enterpriseId or an Enterprise object
        // The MemberRepository would need a method like `findByEnterpriseId(Long enterpriseId)`
        // Or, if using the Enterprise object on Member:
        // Enterprise enterprise = enterpriseRepository.findById(enterpriseId).orElseThrow(...);
        // return memberRepository.findByEnterprise(enterprise);
        // For now, let's assume enterprise.getMembers() could be used if the relationship is bidirectional and managed,
        // but a direct repository method on MemberRepository is cleaner.
        // Adding a simple iteration for now if Enterprise.getMembers() is populated,
        // otherwise a specific query on MemberRepository is better.
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
            .orElseThrow(() -> new ResourceNotFoundException("Enterprise", "id", enterpriseId));
        return enterprise.getMembers(); // Relies on Enterprise entity having a list of members
                                        // and appropriate fetch strategy or transactional context.
                                        // If MemberRepository.findByEnterpriseId is preferred, that should be added.
    }

    @Transactional
    public Member updateMember(Long id, Member memberDetails) {
        Member existingMember = getMemberById(id); // Handles not found

        if (StringUtils.hasText(memberDetails.getName())) {
            existingMember.setName(memberDetails.getName());
        }
        // Email change might need more validation (uniqueness) and processes (e.g., re-verification)
        if (StringUtils.hasText(memberDetails.getEmail()) && !existingMember.getEmail().equals(memberDetails.getEmail())) {
            if (memberRepository.findByEmail(memberDetails.getEmail()).isPresent()) {
                throw new ValidationException("Email '" + memberDetails.getEmail() + "' is already in use.");
            }
            existingMember.setEmail(memberDetails.getEmail());
        }

        // Handle password update: only if a new password is provided
        if (StringUtils.hasText(memberDetails.getPasswordHash())) {
            existingMember.setPasswordHash(passwordEncoder.encode(memberDetails.getPasswordHash()));
        }
        // Note: memberDetails.getPasswordHash() should contain the new raw password if it's being changed.

        return memberRepository.save(existingMember);
    }

    @Transactional
    public void deleteMember(Long id) {
        Member member = getMemberById(id); // Handles not found
        memberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String email, String rawPassword) {
        Optional<Member> memberOpt = getMemberByEmail(email);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            return passwordEncoder.matches(rawPassword, member.getPasswordHash());
        }
        return false;
    }
}
