package com.icegreen.greenmail.marketing.controller;

import com.icegreen.greenmail.marketing.dto.CreateEmailTemplateRequest;
import com.icegreen.greenmail.marketing.dto.EmailTemplateDto;
import com.icegreen.greenmail.marketing.dto.UpdateEmailTemplateRequest;
import com.icegreen.greenmail.marketing.model.entity.EmailTemplate;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.service.MemberService; // To resolve principal to Member
import com.icegreen.greenmail.marketing.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // For principal
import org.springframework.security.core.userdetails.UserDetails; // For principal
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final MemberService memberService; // Added

    @Autowired
    public TemplateController(TemplateService templateService, MemberService memberService) { // Added memberService
        this.templateService = templateService;
        this.memberService = memberService; // Added
    }

    // --- Mapper Methods ---
    public static EmailTemplateDto toDto(EmailTemplate template) {
        if (template == null) return null;
        EmailTemplateDto dto = new EmailTemplateDto();
        dto.setId(template.getId());
        if (template.getMember() != null) {
            dto.setMemberId(template.getMember().getId());
        }
        dto.setName(template.getName());
        dto.setContentHtml(template.getContentHtml());
        dto.setContentText(template.getContentText());
        dto.setCreatedAt(template.getCreatedAt() != null ? template.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(template.getUpdatedAt() != null ? template.getUpdatedAt().toLocalDateTime() : null);
        return dto;
    }

    public static EmailTemplate fromCreateRequest(CreateEmailTemplateRequest request) {
        if (request == null) return null;
        EmailTemplate template = new EmailTemplate();
        template.setName(request.getName());
        template.setContentHtml(request.getContentHtml());
        template.setContentText(request.getContentText());
        // Member will be set by the service using request.getMemberId()
        return template;
    }

    public static EmailTemplate fromUpdateRequest(UpdateEmailTemplateRequest request, EmailTemplate existingTemplate) {
        // Apply updates from request to an existing entity, service layer will fetch existing
        if (request == null || existingTemplate == null) return existingTemplate;
        existingTemplate.setName(request.getName());
        existingTemplate.setContentHtml(request.getContentHtml());
        existingTemplate.setContentText(request.getContentText());
        return existingTemplate;
    }

    // --- Controller Methods ---

    @PostMapping
    public ResponseEntity<EmailTemplateDto> createTemplate(
            @Valid @RequestBody CreateEmailTemplateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        EmailTemplate templateToCreate = fromCreateRequest(request);
        EmailTemplate createdTemplate = templateService.createTemplate(templateToCreate, authenticatedMember.getId());
        return new ResponseEntity<>(toDto(createdTemplate), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> getTemplateById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        EmailTemplate template = templateService.getTemplateById(id);
        // Ownership check
        if (!Objects.equals(template.getMember().getId(), authenticatedMember.getId())) {
            // Or throw ResourceNotFoundException to obscure existence
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toDto(template));
    }

    @GetMapping("/my-templates") // Changed from /by-member/{memberId}
    public ResponseEntity<List<EmailTemplateDto>> getMyTemplates(
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        List<EmailTemplate> templates = templateService.getTemplatesByMemberId(authenticatedMember.getId());
        List<EmailTemplateDto> dtos = templates.stream()
                .map(TemplateController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailTemplateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        EmailTemplate templateDetails = new EmailTemplate();
        templateDetails.setName(request.getName());
        templateDetails.setContentHtml(request.getContentHtml());
        templateDetails.setContentText(request.getContentText());

        EmailTemplate updatedTemplate = templateService.updateTemplate(id, templateDetails, authenticatedMember.getId());
        return ResponseEntity.ok(toDto(updatedTemplate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        templateService.deleteTemplate(id, authenticatedMember.getId());
        return ResponseEntity.noContent().build();
    }
}
