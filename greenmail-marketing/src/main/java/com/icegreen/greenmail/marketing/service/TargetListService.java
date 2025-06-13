package com.icegreen.greenmail.marketing.service;

import com.icegreen.greenmail.marketing.exception.ResourceNotFoundException;
import com.icegreen.greenmail.marketing.exception.ValidationException;
import com.icegreen.greenmail.marketing.model.entity.Member;
import com.icegreen.greenmail.marketing.model.entity.TargetList;
import com.icegreen.greenmail.marketing.model.entity.TargetListContact;
import com.icegreen.greenmail.marketing.repository.MemberRepository;
import com.icegreen.greenmail.marketing.repository.TargetListContactRepository;
import com.icegreen.greenmail.marketing.repository.TargetListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.util.List;
import java.util.Objects;

@Service
public class TargetListService {

    private final TargetListRepository targetListRepository;
    private final TargetListContactRepository targetListContactRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public TargetListService(TargetListRepository targetListRepository,
                             TargetListContactRepository targetListContactRepository,
                             MemberRepository memberRepository) {
        this.targetListRepository = targetListRepository;
        this.targetListContactRepository = targetListContactRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public TargetList createTargetList(TargetList targetList, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "id", memberId));
        targetList.setMember(member);
        return targetListRepository.save(targetList);
    }

    @Transactional(readOnly = true)
    public TargetList getTargetListById(Long id) {
        return targetListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TargetList", "id", id));
    }

    @Transactional(readOnly = true)
    public TargetList getTargetListByIdAndMemberId(Long id, Long memberId) {
        TargetList list = getTargetListById(id);
        if (!Objects.equals(list.getMember().getId(), memberId)) {
            throw new ResourceNotFoundException("TargetList", "id", id + " (for member " + memberId + ")");
        }
        return list;
    }


    @Transactional(readOnly = true)
    public List<TargetList> getTargetListsByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", "id", memberId);
        }
        return targetListRepository.findByMemberId(memberId);
    }

    @Transactional
    public TargetList updateTargetList(Long id, TargetList targetListDetails, Long memberId) {
        TargetList existingList = getTargetListByIdAndMemberId(id, memberId); // Verifies ownership

        if (StringUtils.hasText(targetListDetails.getName())) {
            existingList.setName(targetListDetails.getName());
        }
        return targetListRepository.save(existingList);
    }

    @Transactional
    public void deleteTargetList(Long id, Long memberId) {
        TargetList list = getTargetListByIdAndMemberId(id, memberId); // Verifies ownership
        // Cascading delete should handle TargetListContacts and CampaignTargetList associations
        // as defined in the schema (ON DELETE CASCADE for target_list_id).
        targetListRepository.delete(list);
    }

    @Transactional
    public TargetListContact addContactToTargetList(Long targetListId, TargetListContact contact, Long memberId) {
        TargetList list = getTargetListByIdAndMemberId(targetListId, memberId); // Verifies ownership

        // Check for duplicate email in this specific list
        if (targetListContactRepository.findByTargetListIdAndEmailAddress(targetListId, contact.getEmailAddress()).isPresent()) {
            throw new ValidationException("Contact with email '" + contact.getEmailAddress() + "' already exists in this target list.");
        }
        contact.setTargetList(list);
        return targetListContactRepository.save(contact);
    }

    @Transactional
    public void removeContactFromTargetList(Long targetListId, Long contactId, Long memberId) {
        TargetList list = getTargetListByIdAndMemberId(targetListId, memberId); // Verifies ownership

        TargetListContact contact = targetListContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("TargetListContact", "id", contactId));

        if (!Objects.equals(contact.getTargetList().getId(), list.getId())) {
            throw new ValidationException("Contact does not belong to the specified target list.");
        }
        targetListContactRepository.delete(contact);
    }

    @Transactional(readOnly = true)
    public List<TargetListContact> getContactsByTargetListId(Long targetListId, Long memberId) {
        TargetList list = getTargetListByIdAndMemberId(targetListId, memberId); // Verifies ownership
        return targetListContactRepository.findByTargetListId(list.getId());
    }

    @Transactional
    public TargetListContact updateContactInTargetList(Long targetListId, Long contactId, TargetListContact contactDetails, Long memberId) {
        TargetList list = getTargetListByIdAndMemberId(targetListId, memberId); // Verifies ownership

        TargetListContact existingContact = targetListContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("TargetListContact", "id", contactId));

        if (!Objects.equals(existingContact.getTargetList().getId(), list.getId())) {
            throw new ValidationException("Contact does not belong to the specified target list.");
        }

        // Update fields
        if (StringUtils.hasText(contactDetails.getEmailAddress()) && !existingContact.getEmailAddress().equals(contactDetails.getEmailAddress())) {
             // Check for duplicate if email is changed
            if (targetListContactRepository.findByTargetListIdAndEmailAddress(targetListId, contactDetails.getEmailAddress()).isPresent()) {
                throw new ValidationException("Contact with email '" + contactDetails.getEmailAddress() + "' already exists in this target list.");
            }
            existingContact.setEmailAddress(contactDetails.getEmailAddress());
        }
        if (StringUtils.hasText(contactDetails.getFirstName())) {
            existingContact.setFirstName(contactDetails.getFirstName());
        }
        if (StringUtils.hasText(contactDetails.getLastName())) {
            existingContact.setLastName(contactDetails.getLastName());
        }
        if (contactDetails.getCustomFields() != null) { // Assuming string representation of JSON
            existingContact.setCustomFields(contactDetails.getCustomFields());
        }
        // subscribed status can also be updated
        existingContact.setSubscribed(contactDetails.isSubscribed());

        return targetListContactRepository.save(existingContact);
    }
}
