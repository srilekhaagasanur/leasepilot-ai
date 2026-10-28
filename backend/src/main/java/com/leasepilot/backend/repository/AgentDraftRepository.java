package com.leasepilot.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.ApprovalStatus;

public interface AgentDraftRepository extends JpaRepository<AgentDraft, Long> {

    // The manager's approval queue: drafts for their properties with a given status, oldest first
    List<AgentDraft> findByRequestUnitPropertyManagerIdAndApprovalStatusOrderByCreatedAtAsc(
            Long managerId, ApprovalStatus approvalStatus);

    Optional<AgentDraft> findByRequestId(Long requestId);
}
