package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.SentEmailLog;
import com.icegreen.greenmail.marketing.model.enums.SentEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SentEmailLogRepository extends JpaRepository<SentEmailLog, Long> {
    List<SentEmailLog> findByCampaignRoundId(Long campaignRoundId);
    List<SentEmailLog> findByContactId(Long contactId);
    List<SentEmailLog> findByEmailAccountId(Long emailAccountId);
    List<SentEmailLog> findByStatus(SentEmailStatus status);
    List<SentEmailLog> findByMessageIdHeader(String messageIdHeader);
}
