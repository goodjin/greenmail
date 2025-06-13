package com.icegreen.greenmail.marketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.marketing.dto.CreateCampaignRequest;
import com.icegreen.greenmail.marketing.model.entity.Campaign;
import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.model.entity.TargetList;
import com.icegreen.greenmail.marketing.repository.CampaignRepository;
import com.icegreen.greenmail.marketing.repository.EnterpriseRepository;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import com.icegreen.greenmail.marketing.repository.TargetListRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private TargetListRepository targetListRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member testMember;
    private String testMemberPassword = "password123";
    private TargetList testTargetList;

    @BeforeEach
    void setUp() {
        // Clear relevant data or rely on @Transactional for rollback
        campaignRepository.deleteAll();
        targetListRepository.deleteAll();
        memberRepository.deleteAll();
        enterpriseRepository.deleteAll();


        Enterprise enterprise = enterpriseRepository.save(new Enterprise("CampaignTestEnt"));

        Member member = new Member();
        member.setEnterprise(enterprise);
        member.setName("Campaign User");
        member.setEmail("campaign.user@example.com");
        member.setPasswordHash(passwordEncoder.encode(testMemberPassword));
        testMember = memberRepository.save(member);

        TargetList targetList = new TargetList();
        targetList.setMember(testMember);
        targetList.setName("Test Target List for Campaign");
        testTargetList = targetListRepository.save(targetList);
    }

    @Test
    void createCampaign_success() throws Exception {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("My New Marketing Campaign");
        request.setDescription("A great campaign to test things.");
        request.setTargetListIds(Collections.singletonList(testTargetList.getId()));

        mockMvc.perform(post("/api/marketing/campaigns")
                .with(httpBasic(testMember.getEmail(), testMemberPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My New Marketing Campaign"))
                .andExpect(jsonPath("$.memberId").value(testMember.getId()))
                .andExpect(jsonPath("$.targetLists[0].id").value(testTargetList.getId()));

        List<Campaign> campaigns = campaignRepository.findByMemberId(testMember.getId());
        assertThat(campaigns).hasSize(1);
        Campaign createdCampaign = campaigns.get(0);
        assertThat(createdCampaign.getName()).isEqualTo("My New Marketing Campaign");
        assertThat(createdCampaign.getTargetLists()).hasSize(1);
        assertThat(createdCampaign.getTargetLists().iterator().next().getId()).isEqualTo(testTargetList.getId());
    }

    @Test
    void createCampaign_unauthorized() throws Exception {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Unauthorized Campaign");
        request.setTargetListIds(Collections.singletonList(testTargetList.getId()));

        mockMvc.perform(post("/api/marketing/campaigns")
                // No authentication
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCampaign_targetListNotFound_shouldFail() throws Exception {
        CreateCampaignRequest request = new CreateCampaignRequest();
        request.setName("Campaign With Invalid Target List");
        request.setTargetListIds(Collections.singletonList(9999L)); // Non-existent ID

        mockMvc.perform(post("/api/marketing/campaigns")
                .with(httpBasic(testMember.getEmail(), testMemberPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()) // Expect 404 due to ResourceNotFoundException for TargetList
                .andExpect(jsonPath("$.message").value("TargetList not found with id : '9999'"));
    }

    @Test
    void getMyCampaigns_success() throws Exception {
        // Create a campaign for the testMember
        Campaign campaign = new Campaign();
        campaign.setMember(testMember);
        campaign.setName("Fetched Campaign");
        campaign.setTargetLists(new HashSet<>(Collections.singletonList(testTargetList)));
        campaignRepository.save(campaign);

        mockMvc.perform(get("/api/marketing/campaigns/my-campaigns")
                .with(httpBasic(testMember.getEmail(), testMemberPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Fetched Campaign"))
                .andExpect(jsonPath("$[0].memberId").value(testMember.getId()));
    }
}
