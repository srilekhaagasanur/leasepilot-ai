package com.leasepilot.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leasepilot.backend.dto.AgentDraftResponse;
import com.leasepilot.backend.dto.DecisionRequest;
import com.leasepilot.backend.dto.TenancyResponse;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.service.ApprovalService;

import jakarta.validation.Valid;

// The manager's approval queue. No login yet, so each call says which manager is acting (managerId).
@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ApprovalService approvalService;

    public ManagerController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // GET /api/manager/drafts?managerId=1            -> drafts waiting for approval
    // GET /api/manager/drafts?managerId=1&status=APPROVED -> past decisions
    @GetMapping("/drafts")
    public List<AgentDraftResponse> drafts(@RequestParam Long managerId,
                                           @RequestParam(defaultValue = "PENDING") ApprovalStatus status) {
        return approvalService.draftQueue(managerId, status);
    }

    // POST /api/manager/drafts/{id}/approve   body: {"managerId": 1, "note": "optional"}
    @PostMapping("/drafts/{draftId}/approve")
    public AgentDraftResponse approveDraft(@PathVariable Long draftId, @Valid @RequestBody DecisionRequest body) {
        return approvalService.approveDraft(draftId, body.managerId(), body.note());
    }

    // POST /api/manager/drafts/{id}/reject    body: {"managerId": 1, "note": "why"}
    @PostMapping("/drafts/{draftId}/reject")
    public AgentDraftResponse rejectDraft(@PathVariable Long draftId, @Valid @RequestBody DecisionRequest body) {
        return approvalService.rejectDraft(draftId, body.managerId(), body.note());
    }

    // POST /api/manager/requests/{id}/rerun-agent   body: {"managerId": 1}
    @PostMapping("/requests/{requestId}/rerun-agent")
    public AgentDraftResponse rerunAgent(@PathVariable Long requestId, @Valid @RequestBody DecisionRequest body) {
        return approvalService.rerunAgent(requestId, body.managerId());
    }

    // GET /api/manager/tenancies?managerId=1  -> tenants waiting to join a unit
    @GetMapping("/tenancies")
    public List<TenancyResponse> pendingTenancies(@RequestParam Long managerId) {
        return approvalService.pendingTenancies(managerId);
    }

    // POST /api/manager/tenancies/{id}/approve   body: {"managerId": 1}
    @PostMapping("/tenancies/{tenancyId}/approve")
    public TenancyResponse approveTenancy(@PathVariable Long tenancyId, @Valid @RequestBody DecisionRequest body) {
        return approvalService.decideTenancy(tenancyId, body.managerId(), true);
    }

    // POST /api/manager/tenancies/{id}/reject    body: {"managerId": 1}
    @PostMapping("/tenancies/{tenancyId}/reject")
    public TenancyResponse rejectTenancy(@PathVariable Long tenancyId, @Valid @RequestBody DecisionRequest body) {
        return approvalService.decideTenancy(tenancyId, body.managerId(), false);
    }
}
