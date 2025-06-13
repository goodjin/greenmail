package com.icegreen.greenmail.marketing.controller;

import com.icegreen.greenmail.marketing.dto.CreateEmailAccountRequest;
import com.icegreen.greenmail.marketing.dto.CreateEmailAccountRequest;
import com.icegreen.greenmail.marketing.dto.EmailAccountDto;
import com.icegreen.greenmail.marketing.dto.UpdateEmailAccountRequest;
import com.icegreen.greenmail.marketing.model.entity.EmailAccount;
import com.icegreen.greenmail.marketing.model.entity.Member; // For principal
import com.icegreen.greenmail.marketing.service.EmailAccountService;
import com.icegreen.greenmail.marketing.service.MemberService; // For principal
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // For principal
import org.springframework.security.core.userdetails.UserDetails; // For principal
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;


import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/email-accounts")
public class EmailAccountController {

    private final EmailAccountService emailAccountService;
    private final MemberService memberService; // Added

    @Autowired
    public EmailAccountController(EmailAccountService emailAccountService, MemberService memberService) { // Added memberService
        this.emailAccountService = emailAccountService;
        this.memberService = memberService; // Added
    }

    // --- Mapper Methods ---
    public static EmailAccountDto toDto(EmailAccount account) {
        if (account == null) return null;
        EmailAccountDto dto = new EmailAccountDto();
        dto.setId(account.getId());
        if (account.getMember() != null) {
            dto.setMemberId(account.getMember().getId());
        }
        dto.setEmailAddress(account.getEmailAddress());
        dto.setSmtpHost(account.getSmtpHost());
        dto.setSmtpPort(account.getSmtpPort());
        dto.setSmtpUsername(account.getSmtpUsername());
        // Password is NOT mapped to DTO
        dto.setSmtpProtocol(account.getSmtpProtocol());
        dto.setCreatedAt(account.getCreatedAt() != null ? account.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(account.getUpdatedAt() != null ? account.getUpdatedAt().toLocalDateTime() : null);
        return dto;
    }

    public static EmailAccount fromCreateRequest(CreateEmailAccountRequest request) {
        if (request == null) return null;
        EmailAccount account = new EmailAccount();
        // Member will be set by the service using request.getMemberId()
        account.setEmailAddress(request.getEmailAddress());
        account.setSmtpHost(request.getSmtpHost());
        account.setSmtpPort(request.getSmtpPort());
        account.setSmtpUsername(request.getSmtpUsername());
        account.setSmtpPassword(request.getSmtpPassword()); // Service handles this (e.g. encryption if any)
        account.setSmtpProtocol(request.getSmtpProtocol());
        return account;
    }

    // For update, service layer will apply non-null fields to existing entity
    public static EmailAccount fromUpdateRequest(UpdateEmailAccountRequest request) {
        if (request == null) return null;
        EmailAccount account = new EmailAccount();
        account.setEmailAddress(request.getEmailAddress()); // Can be null
        account.setSmtpHost(request.getSmtpHost());       // Can be null
        account.setSmtpPort(request.getSmtpPort());       // Can be null
        account.setSmtpUsername(request.getSmtpUsername()); // Can be null
        account.setSmtpPassword(request.getSmtpPassword()); // Can be null
        account.setSmtpProtocol(request.getSmtpProtocol()); // Can be null
        return account;
    }


    // --- Controller Methods ---

    @PostMapping
    public ResponseEntity<EmailAccountDto> createEmailAccount(
            @Valid @RequestBody CreateEmailAccountRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        EmailAccount accountToCreate = fromCreateRequest(request);
        EmailAccount createdAccount = emailAccountService.createEmailAccount(accountToCreate, authenticatedMember.getId());
        return new ResponseEntity<>(toDto(createdAccount), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailAccountDto> getEmailAccountById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        EmailAccount account = emailAccountService.getEmailAccountById(id);
        if (!Objects.equals(account.getMember().getId(), authenticatedMember.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toDto(account));
    }

    @GetMapping("/my-accounts") // Changed from /by-member/{memberId}
    public ResponseEntity<List<EmailAccountDto>> getMyEmailAccounts(
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        List<EmailAccount> accounts = emailAccountService.getEmailAccountsByMemberId(authenticatedMember.getId());
        List<EmailAccountDto> dtos = accounts.stream()
                .map(EmailAccountController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailAccountDto> updateEmailAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailAccountRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        // Ensure account belongs to authenticated user before update
        EmailAccount existingAccount = emailAccountService.getEmailAccountById(id);
         if (!Objects.equals(existingAccount.getMember().getId(), authenticatedMember.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        EmailAccount accountDetails = fromUpdateRequest(request);
        EmailAccount updatedAccount = emailAccountService.updateEmailAccount(id, accountDetails); // Service should re-verify ownership or trust controller
        return ResponseEntity.ok(toDto(updatedAccount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmailAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        // Ensure account belongs to authenticated user before deletion
        EmailAccount existingAccount = emailAccountService.getEmailAccountById(id);
         if (!Objects.equals(existingAccount.getMember().getId(), authenticatedMember.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        emailAccountService.deleteEmailAccount(id);
        return ResponseEntity.noContent().build();
    }
}
