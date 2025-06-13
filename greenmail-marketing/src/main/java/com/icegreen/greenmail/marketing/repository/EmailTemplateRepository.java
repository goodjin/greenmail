package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    List<EmailTemplate> findByMemberId(Long memberId);
}
