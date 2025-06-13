package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.CampaignRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRoundRepository extends JpaRepository<CampaignRound, Long> {
    List<CampaignRound> findByCampaignIdOrderByRoundNumberAsc(Long campaignId);
    Optional<CampaignRound> findByCampaignIdAndRoundNumber(Long campaignId, Integer roundNumber);
}
