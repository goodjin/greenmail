package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {
}
