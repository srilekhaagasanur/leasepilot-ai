package com.leasepilot.backend.dto;

import java.time.Instant;
import java.util.List;

import com.leasepilot.backend.agent.ToolStep;
import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.LeaseClause;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.Responsibility;
import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.Urgency;
import com.leasepilot.backend.model.Vendor;

// One item in the manager's approval queue: the tenant's request plus everything the agent proposed
public record AgentDraftResponse(
        Long draftId,
        Long requestId,
        String unitNumber,
        String tenantName,
        Instant reportedAt,
        // What the tenant sent and what the AI worked out
        String description,
        String transcript,
        boolean hasPhoto,
        String aiSummary,
        ServiceCategory category,
        Urgency urgency,
        String urgencyReason,
        String photoObservations,
        // What the agent proposes
        Responsibility responsibility,
        String citedClauseNumber,
        String citedClauseTitle,
        String citedClauseText,
        String vendorName,
        String vendorPhone,
        String workOrderText,
        String tenantReplyText,
        String reasoning,
        List<ToolStep> toolTrace,
        // The manager's decision
        ApprovalStatus approvalStatus,
        String managerNote,
        Instant decidedAt) {

    public static AgentDraftResponse from(AgentDraft d, List<ToolStep> toolTrace) {
        MaintenanceRequest r = d.getRequest();
        LeaseClause clause = d.getCitedClause();
        Vendor vendor = d.getVendor();
        return new AgentDraftResponse(
                d.getId(), r.getId(), r.getUnit().getUnitNumber(), r.getTenant().getFullName(), r.getCreatedAt(),
                r.getDescription(), r.getTranscript(), r.getPhotoPath() != null,
                r.getAiSummary(), r.getCategory(), r.getUrgency(), r.getUrgencyReason(), r.getPhotoObservations(),
                d.getResponsibility(),
                clause != null ? clause.getClauseNumber() : null,
                clause != null ? clause.getTitle() : null,
                clause != null ? clause.getText() : null,
                vendor != null ? vendor.getName() : null,
                vendor != null ? vendor.getPhone() : null,
                d.getWorkOrderText(), d.getTenantReplyText(), d.getReasoning(), toolTrace,
                d.getApprovalStatus(), d.getManagerNote(), d.getDecidedAt());
    }
}
