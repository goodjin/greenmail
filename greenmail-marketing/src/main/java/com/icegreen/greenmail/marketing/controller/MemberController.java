package com.icegreen.greenmail.marketing.controller;

import com.icegreen.greenmail.marketing.dto.*;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.service.MemberService;
import com.icegreen.greenmail.marketing.exception.ValidationException; // For login failure
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;


import javax.validation.Valid;
import java.util.List;
import java.util.UUID; // For dummy token
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketing/members")
public class MemberController {

    private final MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // --- Mapper Methods ---
    public static MemberDto toDto(Member member) {
        if (member == null) return null;
        return new MemberDto(
                member.getId(),
                member.getEnterprise() != null ? member.getEnterprise().getId() : null,
                member.getName(),
                member.getEmail(),
                member.getCreatedAt() != null ? member.getCreatedAt().toLocalDateTime() : null,
                member.getUpdatedAt() != null ? member.getUpdatedAt().toLocalDateTime() : null
        );
    }

    public static Member fromRegisterRequest(RegisterMemberRequest request) {
        if (request == null) return null;
        Member member = new Member();
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPasswordHash(request.getPassword()); // Service will hash this
        // Enterprise will be set by service
        return member;
    }

    // For update, only map fields that are present in the request
    // The service layer will handle merging with existing entity
    public static Member fromUpdateRequest(UpdateMemberRequest request) {
        if (request == null) return null;
        Member member = new Member();
        if (StringUtils.hasText(request.getName())) {
            member.setName(request.getName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            member.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPassword())) {
            member.setPasswordHash(request.getPassword()); // Service will hash this
        }
        return member;
    }


    // --- Controller Methods ---

    @PostMapping("/register")
    public ResponseEntity<MemberDto> registerMember(@Valid @RequestBody RegisterMemberRequest request) {
        Member memberToRegister = fromRegisterRequest(request);
        Member registeredMember = memberService.registerMember(memberToRegister, request.getEnterpriseId());
        return new ResponseEntity<>(toDto(registeredMember), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<MemberDto> login(@Valid @RequestBody LoginRequest request) {
        // Spring Security with HttpBasic or FormLogin handles actual authentication.
        // This endpoint now effectively serves as a "get authenticated user details after login"
        // or can be removed if Spring Security's success handlers manage redirection/response.
        // For a REST API that might not rely on session cookies post-login (e.g. if it were to issue a token),
        // this endpoint, if called after successful basic/form auth, could return user details.
        // If Spring Security handles the login entirely (e.g. formLogin processes POST to /api/marketing/members/login),
        // this explicit controller method might not even be hit for the actual credential check.
        // However, UserDetailsService IS used by Spring Security.
        // For now, let's assume it's called upon successful basic auth or as a separate step.

        boolean isAuthenticated = memberService.authenticate(request.getEmail(), request.getPassword());
        if (isAuthenticated) {
            Member member = memberService.getMemberByEmailOrThrow(request.getEmail());
            // LoginResponse used to include a token. Now, just returning MemberDto.
            // Token generation/management would be a separate concern (e.g., JWT filter).
            return ResponseEntity.ok(toDto(member));
        } else {
            throw new ValidationException("Invalid email or password");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberDto> getMemberById(@PathVariable Long id) {
        Member member = memberService.getMemberById(id);
        return ResponseEntity.ok(toDto(member));
    }

    @GetMapping("/by-enterprise/{enterpriseId}")
    public ResponseEntity<List<MemberDto>> getMembersByEnterprise(
            @PathVariable Long enterpriseId,
            @AuthenticationPrincipal UserDetails principal) { // Assuming admin or member of same enterprise might see this
        // TODO: Add logic here to check if authenticated principal has rights to view members of this enterprise.
        // For now, proceeding with the fetch. This endpoint might be admin-only.
        List<Member> members = memberService.getMembersByEnterpriseId(enterpriseId);
        List<MemberDto> memberDtos = members.stream()
                .map(MemberController::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(memberDtos);
    }

    // This endpoint might be admin-only or for specific lookups.
    // For a user to get their own details, a "/me" endpoint is better.
    @GetMapping("/by-email")
    public ResponseEntity<MemberDto> getMemberByEmail(
            @RequestParam String email,
            @AuthenticationPrincipal UserDetails principal) {
        // TODO: Add security check: is principal asking for their own email or are they an admin?
        Member member = memberService.getMemberByEmailOrThrow(email);
        // Example check:
        // if (!principal.getUsername().equals(email) /* && !isAdmin(principal) */ ) {
        //    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // }
        return ResponseEntity.ok(toDto(member));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberDto> getMyDetails(@AuthenticationPrincipal UserDetails principal) {
        Member member = memberService.getMemberByEmailOrThrow(principal.getUsername());
        return ResponseEntity.ok(toDto(member));
    }


    @PutMapping("/me") // Changed from /api/marketing/members/{id} to operate on authenticated user
    public ResponseEntity<MemberDto> updateMyDetails(
            @Valid @RequestBody UpdateMemberRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        Member memberDetails = fromUpdateRequest(request);
        Member updatedMember = memberService.updateMember(authenticatedMember.getId(), memberDetails);
        return ResponseEntity.ok(toDto(updatedMember));
    }

    // DELETE /{id} might be an admin operation. Deleting one's own account could be /me/delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        // TODO: Add security check: is principal deleting their own account or are they an admin?
        // For self-delete:
        // Member authenticatedMember = memberService.getMemberByEmailOrThrow(principal.getUsername());
        // if (!authenticatedMember.getId().equals(id)) {
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // }
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }
}
