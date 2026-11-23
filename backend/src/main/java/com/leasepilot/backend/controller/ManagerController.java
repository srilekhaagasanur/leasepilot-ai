package com.leasepilot.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.leasepilot.backend.dto.AgentDraftResponse;
import com.leasepilot.backend.dto.DecisionRequest;
import com.leasepilot.backend.dto.ProcessingRequestResponse;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.security.AppUserPrincipal;
import com.leasepilot.backend.service.ApprovalService;

// The manager's approval queue. The manager is whoever is logged in (only managers can call these).
@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ApprovalService approvalService;

    public ManagerController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // GET /api/manager/drafts                  -> drafts waiting for approval
    // GET /api/manager/drafts?status=APPROVED  -> past decisions
    @GetMapping("/drafts")
    public List<AgentDraftResponse> drafts(@AuthenticationPrincipal AppUserPrincipal me,
                                           @RequestParam(defaultValue = "PENDING") ApprovalStatus status) {
        return approvalService.draftQueue(me.id(), status);
    }

    // POST /api/manager/drafts/{id}/approve   body (optional): {"note": "..."}
    @PostMapping("/drafts/{draftId}/approve")
    public AgentDraftResponse approveDraft(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long draftId,
                                           @RequestBody(required = false) DecisionRequest body) {
        return approvalService.approveDraft(draftId, me.id(), noteOf(body));
    }

    // POST /api/manager/drafts/{id}/reject    body (optional): {"note": "why"}
    @PostMapping("/drafts/{draftId}/reject")
    public AgentDraftResponse rejectDraft(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long draftId,
                                          @RequestBody(required = false) DecisionRequest body) {
        return approvalService.rejectDraft(draftId, me.id(), noteOf(body));
    }

    // GET /api/manager/requests/unfinished  -> requests the AI is still preparing, or couldn't finish
    @GetMapping("/requests/unfinished")
    public List<ProcessingRequestResponse> unfinishedRequests(@AuthenticationPrincipal AppUserPrincipal me) {
        return approvalService.unfinishedRequests(me.id());
    }

    // POST /api/manager/requests/{id}/retry-ai  -> run the AI again on a request it couldn't finish
    @PostMapping("/requests/{requestId}/retry-ai")
    public ProcessingRequestResponse retryAi(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long requestId) {
        return approvalService.retryAi(requestId, me.id());
    }

    private static String noteOf(DecisionRequest body) {
        return body != null ? body.note() : null;
    }
}
