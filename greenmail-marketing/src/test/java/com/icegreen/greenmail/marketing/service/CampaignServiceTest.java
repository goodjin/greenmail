package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.exception.ValidationException;
import com.icegreen.greenmail.marketing.model.entity.*;
import com.icegreen.greenmail.marketing.model.enums.CampaignStatus;
import com.icegreen.greenmail.marketing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.HashSet;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignRoundRepository campaignRoundRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private EmailTemplateRepository emailTemplateRepository;
    @Mock
    private TargetListRepository targetListRepository;

    @InjectMocks
    private CampaignService campaignService;

    private Member member;
    private Campaign campaign;
    private TargetList targetList1;
    private EmailTemplate emailTemplate;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(1L);
        member.setEmail("owner@example.com");

        campaign = new Campaign();
        campaign.setName("Test Campaign");
        campaign.setMember(member);

        targetList1 = new TargetList();
        targetList1.setId(10L);
        targetList1.setName("List 1");
        targetList1.setMember(member); // Important: target list owned by the same member

        emailTemplate = new EmailTemplate();
        emailTemplate.setId(100L);
        emailTemplate.setName("Test Template");
        emailTemplate.setMember(member);
    }

    @Test
    void createCampaign_success() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(targetListRepository.findById(10L)).thenReturn(Optional.of(targetList1));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign savedCampaign = invocation.getArgument(0);
            savedCampaign.setId(1L); // Simulate save
            return savedCampaign;
        });

        Campaign newCampaign = new Campaign();
        newCampaign.setName("New Campaign");

        Campaign created = campaignService.createCampaign(newCampaign, Collections.singletonList(10L), 1L);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("New Campaign");
        assertThat(created.getMember()).isEqualTo(member);
        assertThat(created.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(created.getTargetLists()).hasSize(1);
        assertThat(created.getTargetLists().iterator().next().getId()).isEqualTo(10L);
        verify(campaignRepository).save(newCampaign);
    }

    @Test
    void createCampaign_memberNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        Campaign newCampaign = new Campaign();
        newCampaign.setName("New Campaign");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            campaignService.createCampaign(newCampaign, Collections.singletonList(10L), 1L);
        });
        assertThat(exception.getMessage()).contains("Member not found with id : '1'");
    }

    @Test
    void createCampaign_targetListNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(targetListRepository.findById(10L)).thenReturn(Optional.empty());

        Campaign newCampaign = new Campaign();
        newCampaign.setName("New Campaign");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            campaignService.createCampaign(newCampaign, Collections.singletonList(10L), 1L);
        });
        assertThat(exception.getMessage()).contains("TargetList not found with id : '10'");
    }

    @Test
    void createCampaign_targetListNotOwnedByMember() {
        Member anotherMember = new Member();
        anotherMember.setId(2L);
        targetList1.setMember(anotherMember); // Target list owned by a different member

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(targetListRepository.findById(10L)).thenReturn(Optional.of(targetList1));

        Campaign newCampaign = new Campaign();
        newCampaign.setName("New Campaign");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            campaignService.createCampaign(newCampaign, Collections.singletonList(10L), 1L);
        });
        assertThat(exception.getMessage()).contains("TargetList with id 10 does not belong to the member.");
    }

    @Test
    void addRoundToCampaign_success() {
        campaign.setId(1L); // Existing campaign
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(emailTemplateRepository.findById(100L)).thenReturn(Optional.of(emailTemplate));
        when(campaignRoundRepository.findByCampaignIdAndRoundNumber(1L, 1)).thenReturn(Optional.empty());
        when(campaignRoundRepository.save(any(CampaignRound.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignRound newRound = new CampaignRound();
        newRound.setRoundNumber(1);
        newRound.setSubjectTemplate("Subject for Round 1");
        newRound.setEmailTemplate(emailTemplate); // Set the template object with ID

        CampaignRound addedRound = campaignService.addRoundToCampaign(1L, newRound, 1L);

        assertThat(addedRound).isNotNull();
        assertThat(addedRound.getCampaign()).isEqualTo(campaign);
        assertThat(addedRound.getEmailTemplate().getId()).isEqualTo(100L);
        assertThat(addedRound.getRoundNumber()).isEqualTo(1);
        assertThat(addedRound.getStatus()).isEqualTo(com.icegreen.greenmail.marketing.model.enums.CampaignRoundStatus.PENDING);
        verify(campaignRoundRepository).save(newRound);
    }

    @Test
    void addRoundToCampaign_templateNotOwnedByMember() {
        campaign.setId(1L);
        Member anotherMember = new Member(); // Template owner
        anotherMember.setId(2L);
        emailTemplate.setMember(anotherMember);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign)); // Campaign owner is member with ID 1
        when(emailTemplateRepository.findById(100L)).thenReturn(Optional.of(emailTemplate)); // Template owned by member 2

        CampaignRound newRound = new CampaignRound();
        newRound.setRoundNumber(1);
        newRound.setSubjectTemplate("Subject");
        newRound.setEmailTemplate(emailTemplate);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            campaignService.addRoundToCampaign(1L, newRound, 1L); // Member 1 trying to add round
        });
        assertThat(exception.getMessage()).contains("EmailTemplate not found with id : '100' (for member 1)");
    }
}
