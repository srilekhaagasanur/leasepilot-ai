package com.leasepilot.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.agent.MaintenanceAgent;
import com.leasepilot.backend.agent.ToolStep;
import com.leasepilot.backend.dto.AgentDraftResponse;
import com.leasepilot.backend.dto.TenancyResponse;
import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.model.Unit;
import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.TenancyRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

// Everything the manager approves or rejects: the agent's drafts and tenants' requests to join a unit.
// Nothing reaches a tenant or vendor until it goes through here.
@Service
public class ApprovalService {

    private final AgentDraftRepository draftRepository;
    private final TenancyRepository tenancyRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final MaintenanceAgent maintenanceAgent;
    private final JsonMapper jsonMapper;

    public ApprovalService(AgentDraftRepository draftRepository, TenancyRepository tenancyRepository,
                           MaintenanceRequestRepository requestRepository, MaintenanceAgent maintenanceAgent,
                           JsonMapper jsonMapper) {
        this.draftRepository = draftRepository;
        this.tenancyRepository = tenancyRepository;
        this.requestRepository = requestRepository;
        this.maintenanceAgent = maintenanceAgent;
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
        return toResponse(draft);
    }

    @Transactional
    public AgentDraftResponse rejectDraft(Long draftId, Long managerId, String note) {
        AgentDraft draft = pendingDraftOwnedBy(draftId, managerId);
        draft.reject(note);
        draft.getRequest().setStatus(RequestStatus.REJECTED);
        return toResponse(draft);
    }

    // Runs the agent again for a request (e.g. after it failed, or the manager rejected its draft)
    public AgentDraftResponse rerunAgent(Long requestId, Long managerId) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        checkManages(request.getUnit(), managerId);
        if (request.getStatus() == RequestStatus.APPROVED || request.getStatus() == RequestStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request was already approved");
        }
        if (request.getCategory() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request hasn't been analyzed yet");
        }
        return toResponse(maintenanceAgent.run(request));
    }

    // --- Tenants joining units ---

    public List<TenancyResponse> pendingTenancies(Long managerId) {
        return tenancyRepository.findByUnitPropertyManagerIdAndStatus(managerId, ApprovalStatus.PENDING).stream()
                .map(TenancyResponse::from)
                .toList();
    }

    @Transactional
    public TenancyResponse decideTenancy(Long tenancyId, Long managerId, boolean approve) {
        Tenancy tenancy = tenancyRepository.findById(tenancyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));
        checkManages(tenancy.getUnit(), managerId);
        if (tenancy.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This join request was already decided");
        }
        if (approve) {
            tenancy.approve();
        } else {
            tenancy.reject();
        }
        return TenancyResponse.from(tenancy);
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
