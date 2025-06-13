package com.icegreen.greenmail.marketing.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.marketing.dto.*;
import com.icegreen.greenmail.marketing.model.entity.TargetList;
import com.icegreen.greenmail.marketing.model.entity.TargetListContact;
import com.icegreen.greenmail.marketing.model.entity.Member; // For principal
import com.icegreen.greenmail.marketing.service.MemberService; // For principal
import com.icegreen.greenmail.marketing.service.TargetListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // For principal
import org.springframework.security.core.userdetails.UserDetails; // For principal
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;


import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/target-lists")
public class TargetListController {
    private static final Logger logger = LoggerFactory.getLogger(TargetListController.class);

    private final TargetListService targetListService;
    private final MemberService memberService; // Added
    private final ObjectMapper objectMapper;

    @Autowired
    public TargetListController(TargetListService targetListService, MemberService memberService, ObjectMapper objectMapper) { // Added memberService
        this.targetListService = targetListService;
        this.memberService = memberService; // Added
        this.objectMapper = objectMapper;
    }

    // --- Mapper Methods for TargetList ---
    public static TargetListDto toDto(TargetList targetList) {
        if (targetList == null) return null;
        TargetListDto dto = new TargetListDto();
        dto.setId(targetList.getId());
        if (targetList.getMember() != null) {
            dto.setMemberId(targetList.getMember().getId());
        }
        dto.setName(targetList.getName());
        dto.setCreatedAt(targetList.getCreatedAt() != null ? targetList.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(targetList.getUpdatedAt() != null ? targetList.getUpdatedAt().toLocalDateTime() : null);
        return dto;
    }

    public static TargetList fromCreateRequest(CreateTargetListRequest request) {
        if (request == null) return null;
        TargetList list = new TargetList();
        list.setName(request.getName());
        // Member set by service
        return list;
    }

    // --- Mapper Methods for TargetListContact ---
    @SuppressWarnings("unchecked")
    public TargetListContactDto toDto(TargetListContact contact) {
        if (contact == null) return null;
        TargetListContactDto dto = new TargetListContactDto();
        dto.setId(contact.getId());
        if (contact.getTargetList() != null) {
            dto.setTargetListId(contact.getTargetList().getId());
        }
        dto.setEmailAddress(contact.getEmailAddress());
        dto.setFirstName(contact.getFirstName());
        dto.setLastName(contact.getLastName());
        dto.setSubscribed(contact.isSubscribed());
        dto.setCreatedAt(contact.getCreatedAt() != null ? contact.getCreatedAt().toLocalDateTime() : null);
        dto.setUpdatedAt(contact.getUpdatedAt() != null ? contact.getUpdatedAt().toLocalDateTime() : null);
        if (StringUtils.hasText(contact.getCustomFields())) {
            try {
                dto.setCustomFields(objectMapper.readValue(contact.getCustomFields(), Map.class));
            } catch (JsonProcessingException e) {
                logger.error("Error parsing custom_fields JSON for contact id {}: {}", contact.getId(), e.getMessage());
                // Set to null or empty map if parsing fails
                dto.setCustomFields(null);
            }
        }
        return dto;
    }

    public TargetListContact fromCreateRequest(CreateTargetListContactRequest request) {
        if (request == null) return null;
        TargetListContact contact = new TargetListContact();
        contact.setEmailAddress(request.getEmailAddress());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        if (request.getSubscribed() != null) { // Default is true in entity
            contact.setSubscribed(request.getSubscribed());
        }
        if (request.getCustomFields() != null && !request.getCustomFields().isEmpty()) {
            try {
                contact.setCustomFields(objectMapper.writeValueAsString(request.getCustomFields()));
            } catch (JsonProcessingException e) {
                logger.error("Error serializing custom_fields JSON for new contact {}: {}", request.getEmailAddress(), e.getMessage());
                // Or throw validation exception
            }
        }
        // TargetList set by service
        return contact;
    }

    public TargetListContact fromUpdateRequest(UpdateTargetListContactRequest request, TargetListContact existingContact) {
        if (request == null || existingContact == null) return existingContact;

        if (StringUtils.hasText(request.getEmailAddress())) {
            existingContact.setEmailAddress(request.getEmailAddress());
        }
        if (request.getFirstName() != null) { // Allow clearing with empty string if desired, or add specific handling
            existingContact.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            existingContact.setLastName(request.getLastName());
        }
        if (request.getSubscribed() != null) {
            existingContact.setSubscribed(request.getSubscribed());
        }
        if (request.getCustomFields() != null) {
            try {
                existingContact.setCustomFields(objectMapper.writeValueAsString(request.getCustomFields()));
            } catch (JsonProcessingException e) {
                 logger.error("Error serializing custom_fields JSON for contact id {}: {}", existingContact.getId(), e.getMessage());
            }
        }
        return existingContact;
    }


    // --- TargetList Endpoints ---

    @PostMapping
    public ResponseEntity<TargetListDto> createTargetList(
            @Valid @RequestBody CreateTargetListRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        TargetList listToCreate = fromCreateRequest(request);
        TargetList createdList = targetListService.createTargetList(listToCreate, authenticatedMember.getId());
        return new ResponseEntity<>(toDto(createdList), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TargetListDto> getTargetListById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        TargetList list = targetListService.getTargetListByIdAndMemberId(id, authenticatedMember.getId());
        return ResponseEntity.ok(toDto(list));
    }

    @GetMapping("/my-lists") // Changed from /by-member/{memberId}
    public ResponseEntity<List<TargetListDto>> getMyTargetLists(
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        List<TargetList> lists = targetListService.getTargetListsByMemberId(authenticatedMember.getId());
        List<TargetListDto> dtos = lists.stream().map(TargetListController::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TargetListDto> updateTargetList(
            @PathVariable Long id,
            @Valid @RequestBody CreateTargetListRequest request, // Using Create DTO for update (name only)
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        TargetList listDetails = fromCreateRequest(request);
        TargetList updatedList = targetListService.updateTargetList(id, listDetails, authenticatedMember.getId());
        return ResponseEntity.ok(toDto(updatedList));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTargetList(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        targetListService.deleteTargetList(id, authenticatedMember.getId());
        return ResponseEntity.noContent().build();
    }

    // --- TargetListContact Endpoints ---

    @PostMapping("/{targetListId}/contacts")
    public ResponseEntity<TargetListContactDto> addContactToList(
            @PathVariable Long targetListId,
            @Valid @RequestBody CreateTargetListContactRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        TargetListContact contactToCreate = fromCreateRequest(request);
        TargetListContact createdContact = targetListService.addContactToTargetList(targetListId, contactToCreate, authenticatedMember.getId());
        return new ResponseEntity<>(toDto(createdContact), HttpStatus.CREATED);
    }

    @GetMapping("/{targetListId}/contacts")
    public ResponseEntity<List<TargetListContactDto>> getContactsInList(
            @PathVariable Long targetListId,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        List<TargetListContact> contacts = targetListService.getContactsByTargetListId(targetListId, authenticatedMember.getId());
        List<TargetListContactDto> dtos = contacts.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{targetListId}/contacts/{contactId}")
    public ResponseEntity<TargetListContactDto> getSpecificContactInList(
            @PathVariable Long targetListId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        // TargetListService.getContactByIdAndListIdAndMemberId would be ideal here
        // For now, use existing service method for list ownership then fetch contact
        targetListService.getTargetListByIdAndMemberId(targetListId, authenticatedMember.getId());
        TargetListContact contact = targetListService.getTargetListContactRepository().findById(contactId)
             .orElseThrow(() -> new ResourceNotFoundException("TargetListContact", "id", contactId));
        if(!contact.getTargetList().getId().equals(targetListId)){
            throw new ValidationException("Contact does not belong to the specified target list.");
        }
        return ResponseEntity.ok(toDto(contact));
    }


    @PutMapping("/{targetListId}/contacts/{contactId}")
    public ResponseEntity<TargetListContactDto> updateContactInList(
            @PathVariable Long targetListId,
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateTargetListContactRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        TargetListContact contactDetails = new TargetListContact();
        contactDetails.setEmailAddress(request.getEmailAddress());
        contactDetails.setFirstName(request.getFirstName());
        contactDetails.setLastName(request.getLastName());
        contactDetails.setSubscribed(request.getSubscribed() != null ? request.getSubscribed() : true); // Default if not provided might be handled in service or entity
         if (request.getCustomFields() != null) {
            try {
                contactDetails.setCustomFields(objectMapper.writeValueAsString(request.getCustomFields()));
            } catch (JsonProcessingException e) {
                 logger.error("Error serializing custom_fields JSON for contact update {}: {}", contactId, e.getMessage());
            }
        }

        TargetListContact updatedContact = targetListService.updateContactInTargetList(targetListId, contactId, contactDetails, memberId);
        return ResponseEntity.ok(toDto(updatedContact));
    }

    @DeleteMapping("/{targetListId}/contacts/{contactId}")
    public ResponseEntity<Void> removeContactFromList(
            @PathVariable Long targetListId,
            @PathVariable Long contactId,
            @RequestHeader("X-Member-Id") Long memberId) { // Placeholder
        targetListService.removeContactFromTargetList(targetListId, contactId, memberId);
        return ResponseEntity.noContent().build();
    }
}
