package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.TargetListContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetListContactRepository extends JpaRepository<TargetListContact, Long> {
    List<TargetListContact> findByTargetListId(Long targetListId);
    Optional<TargetListContact> findByTargetListIdAndEmailAddress(Long targetListId, String emailAddress);
    List<TargetListContact> findByEmailAddress(String emailAddress);
}
