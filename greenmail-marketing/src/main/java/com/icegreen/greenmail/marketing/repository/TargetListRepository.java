package com.icegreen.greenmail.marketing.repository;

import com.icegreen.greenmail.marketing.model.entity.TargetList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetListRepository extends JpaRepository<TargetList, Long> {
    List<TargetList> findByMemberId(Long memberId);
}
