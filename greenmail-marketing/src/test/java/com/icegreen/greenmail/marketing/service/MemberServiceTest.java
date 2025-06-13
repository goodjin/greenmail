package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.exception.ValidationException;
import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.repository.EnterpriseRepository;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.ArrayList; // For enterprise.getMembers()

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private Enterprise enterprise;

    @BeforeEach
    void setUp() {
        enterprise = new Enterprise("Test Enterprise");
        enterprise.setId(1L);
        // Initialize members list for enterprise.getMembers() in getMembersByEnterpriseId
        enterprise.setMembers(new ArrayList<>());


        member = new Member();
        member.setId(1L);
        member.setName("Test User");
        member.setEmail("test@example.com");
        member.setPasswordHash("rawPassword"); // Service will encode this
        member.setEnterprise(enterprise);
    }

    @Test
    void registerMember_success() {
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(enterprise));
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member savedMember = invocation.getArgument(0);
            savedMember.setId(1L); // Simulate save
            return savedMember;
        });

        Member registeredMember = memberService.registerMember(member, 1L);

        assertThat(registeredMember).isNotNull();
        assertThat(registeredMember.getEmail()).isEqualTo("test@example.com");
        assertThat(registeredMember.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(registeredMember.getEnterprise()).isEqualTo(enterprise);
        verify(memberRepository).save(member);
    }

    @Test
    void registerMember_emailExists() {
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            memberService.registerMember(member, 1L);
        });

        assertThat(exception.getMessage()).isEqualTo("Email 'test@example.com' is already registered.");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void registerMember_enterpriseNotFound() {
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            memberService.registerMember(member, 1L);
        });
        assertThat(exception.getMessage()).contains("Enterprise not found with id : '1'");
    }


    @Test
    void authenticate_success() {
        member.setPasswordHash("encodedPassword"); // Assume it's already encoded in DB
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        boolean isAuthenticated = memberService.authenticate("test@example.com", "rawPassword");

        assertThat(isAuthenticated).isTrue();
    }

    @Test
    void authenticate_failure_userNotFound() {
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        boolean isAuthenticated = memberService.authenticate("test@example.com", "rawPassword");

        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void authenticate_failure_wrongPassword() {
        member.setPasswordHash("encodedPassword");
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        boolean isAuthenticated = memberService.authenticate("test@example.com", "wrongPassword");

        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void getMemberById_notFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            memberService.getMemberById(1L);
        });

        assertThat(exception.getMessage()).contains("Member not found with id : '1'");
    }

    @Test
    void getMembersByEnterpriseId_success() {
        // Given enterprise has member in its list
        enterprise.getMembers().add(member);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(enterprise));
        // Note: This test relies on enterprise.getMembers(). If MemberService was changed
        // to use memberRepository.findByEnterpriseId(1L), the mocking would target that instead.

        List<Member> members = memberService.getMembersByEnterpriseId(1L);

        assertThat(members).isNotNull();
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getMembersByEnterpriseId_enterpriseNotFound() {
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            memberService.getMembersByEnterpriseId(1L);
        });
        assertThat(exception.getMessage()).contains("Enterprise not found with id : '1'");
    }
}
