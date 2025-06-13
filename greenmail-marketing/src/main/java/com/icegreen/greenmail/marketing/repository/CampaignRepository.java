package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByMemberId(Long memberId);
}
