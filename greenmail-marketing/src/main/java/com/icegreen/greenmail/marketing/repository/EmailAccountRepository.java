package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.EmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long> {
    List<EmailAccount> findByMemberId(Long memberId);
}
