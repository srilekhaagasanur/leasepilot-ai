package com.leasepilot.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.agent.ToolStep;
import com.leasepilot.backend.dto.AgentDraftResponse;
import com.leasepilot.backend.dto.ProcessingRequestResponse;
import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.AiStatus;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.Unit;
import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

// Everything the manager approves or rejects: the agent's drafts and tenants' requests to join a unit.
// Nothing reaches a tenant or vendor until it goes through here.
@Service
public class ApprovalService {

    private final AgentDraftRepository draftRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final RequestProcessor requestProcessor;
    private final WorkOrderService workOrderService;
    private final JsonMapper jsonMapper;

    public ApprovalService(AgentDraftRepository draftRepository,
                           MaintenanceRequestRepository requestRepository, RequestProcessor requestProcessor,
                           WorkOrderService workOrderService,
                           JsonMapper jsonMapper) {
        this.draftRepository = draftRepository;
        this.requestRepository = requestRepository;
        this.requestProcessor = requestProcessor;
        this.workOrderService = workOrderService;
        this.jsonMapper = jsonMapper;
    }

    // --- Agent drafts ---

    // The queue: most urgent first, then oldest first
    public List<AgentDraftResponse> draftQueue(Long managerId, ApprovalStatus status) {
        return draftRepository.findByRequestUnitPropertyManagerIdAndApprovalStatusOrderByCreatedAtAsc(managerId, status)
                .stream()
                .sorted(Comparator.comparing((AgentDraft d) -> d.getRequest().getUrgency(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AgentDraftResponse approveDraft(Long draftId, Long managerId, String note) {
        AgentDraft draft = pendingDraftOwnedBy(draftId, managerId);
        draft.approve(note);
        // For the demo, approving is what "sends" the work order and reply.
        // The tenant sees the reply on their requests page from now on.
        draft.getRequest().setStatus(RequestStatus.APPROVED);
        // From here on it's a work order the landlord moves forward (see WorkOrderService)
        workOrderService.recordApproval(draft.getRequest(), managerId);
        return toResponse(draft);
    }

    @Transactional
    public AgentDraftResponse rejectDraft(Long draftId, Long managerId, String note) {
        AgentDraft draft = pendingDraftOwnedBy(draftId, managerId);
        draft.reject(note);
        draft.getRequest().setStatus(RequestStatus.REJECTED);
        return toResponse(draft);
    }

    // --- Requests the AI hasn't finished ---

    // Still being worked on, or failed (so the manager can retry)
    public List<ProcessingRequestResponse> unfinishedRequests(Long managerId) {
        return requestRepository.findByUnitPropertyManagerIdAndAiStatusInOrderByCreatedAtDesc(managerId,
                        List.of(AiStatus.PROCESSING, AiStatus.FAILED)).stream()
                .map(ProcessingRequestResponse::from)
                .toList();
    }

    // Runs the AI again on a request it couldn't finish. Steps that already worked are skipped.
    public ProcessingRequestResponse retryAi(Long requestId, Long managerId) {
        MaintenanceRequest request = requestRepository.findWithDetailsById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        checkManages(request.getUnit(), managerId);
        if (request.getAiStatus() != AiStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The AI isn't stuck on this request");
        }
        request.setAiStatus(AiStatus.PROCESSING);
        request.setAiError(null);
        requestRepository.save(request);
        requestProcessor.processInBackground(requestId);
        return ProcessingRequestResponse.from(request);
    }


    // --- Helpers ---

    private AgentDraft pendingDraftOwnedBy(Long draftId, Long managerId) {
        AgentDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        checkManages(draft.getRequest().getUnit(), managerId);
        if (draft.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This draft was already decided");
        }
        return draft;
    }

    // A manager may only act on units in their own properties
    private static void checkManages(Unit unit, Long managerId) {
        if (!unit.getProperty().getManager().getId().equals(managerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't manage this property");
        }
    }

    private AgentDraftResponse toResponse(AgentDraft draft) {
        List<ToolStep> trace = draft.getToolTrace() == null
                ? List.of()
                : jsonMapper.readValue(draft.getToolTrace(), new TypeReference<List<ToolStep>>() { });
        return AgentDraftResponse.from(draft, trace);
    }
}
