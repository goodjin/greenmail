package com.icegreen.greenmail.marketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.marketing.dto.CreateEnterpriseRequest;
import com.icegreen.greenmail.marketing.dto.EnterpriseDto;
import com.icegreen.greenmail.marketing.dto.RegisterMemberRequest;
import com.icegreen.greenmail.marketing.dto.MemberDto;
import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.repository.EnterpriseRepository;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Rollback transactions after each test
public class MemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // To verify password if needed, or for setup

    private Enterprise testEnterprise;

    @BeforeEach
    void setUp() {
        // Clear relevant data or rely on @Transactional rollback
        memberRepository.deleteAll();
        enterpriseRepository.deleteAll();

        // Setup a default enterprise for tests
        Enterprise enterprise = new Enterprise("TestCo");
        testEnterprise = enterpriseRepository.save(enterprise);
    }

    @Test
    void registerMember_success() throws Exception {
        RegisterMemberRequest request = new RegisterMemberRequest();
        request.setEnterpriseId(testEnterprise.getId());
        request.setName("John Doe");
        request.setEmail("john.doe@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/marketing/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.enterpriseId").value(testEnterprise.getId()));

        Member member = memberRepository.findByEmail("john.doe@example.com").orElse(null);
        assertThat(member).isNotNull();
        assertThat(member.getName()).isEqualTo("John Doe");
        assertThat(passwordEncoder.matches("password123", member.getPasswordHash())).isTrue();
    }

    @Test
    void registerMember_emailExists() throws Exception {
        // Pre-register a member
        Member existingMember = new Member();
        existingMember.setEnterprise(testEnterprise);
        existingMember.setName("Jane Doe");
        existingMember.setEmail("jane.doe@example.com");
        existingMember.setPasswordHash(passwordEncoder.encode("password123"));
        memberRepository.save(existingMember);

        RegisterMemberRequest request = new RegisterMemberRequest();
        request.setEnterpriseId(testEnterprise.getId());
        request.setName("John Doe");
        request.setEmail("jane.doe@example.com"); // Existing email
        request.setPassword("newpassword123");

        mockMvc.perform(post("/api/marketing/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Expect 400 due to ValidationException
                .andExpect(jsonPath("$.message").value("Email 'jane.doe@example.com' is already registered."));
    }

    @Test
    void loginAndAccessSecuredEndpoint_success() throws Exception {
        String username = "user@example.com";
        String password = "password123";

        Member member = new Member();
        member.setEnterprise(testEnterprise);
        member.setName("Test User Login");
        member.setEmail(username);
        member.setPasswordHash(passwordEncoder.encode(password));
        memberRepository.save(member);

        // Test login - Spring Security handles this with httpBasic, so a direct call to /login isn't
        // strictly necessary to "establish a session" for subsequent httpBasic requests.
        // We can directly try accessing a secured endpoint.

        // This call to /login is more to check if it returns member details as designed
        mockMvc.perform(post("/api/marketing/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(username));


        // Test accessing a secured endpoint (/me)
        mockMvc.perform(get("/api/marketing/members/me")
                .with(httpBasic(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(username));
    }

    @Test
    void accessSecuredEndpoint_unauthorized() throws Exception {
        mockMvc.perform(get("/api/marketing/members/me"))
                .andExpect(status().isUnauthorized()); // HTTP Basic prompts for auth, results in 401 if none provided
    }
     @Test
    void accessSecuredEndpoint_wrongCredentials() throws Exception {
        String username = "user.wrong@example.com";
        String password = "password123";
        String wrongPassword = "wrongpassword";

        Member member = new Member();
        member.setEnterprise(testEnterprise);
        member.setName("Test User Wrong Creds");
        member.setEmail(username);
        member.setPasswordHash(passwordEncoder.encode(password));
        memberRepository.save(member);

        mockMvc.perform(get("/api/marketing/members/me")
                .with(httpBasic(username, wrongPassword)))
                .andExpect(status().isUnauthorized());
    }
}

// Helper class for login request if not already present or to avoid full DTO in simple map
// class LoginRequest {
//     public String email;
//     public String password;
// }
// For this test, using Map.of directly for login request body.
