package com.leasepilot.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.dto.WorkOrderResponse;
import com.leasepilot.backend.dto.WorkOrderUpdateResponse;
import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.WorkOrderUpdate;
import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.WorkOrderUpdateRepository;

// After a draft is approved, the request becomes a work order:
//   Approved -> Scheduled (with a date) -> In progress -> Fixed
// The landlord can jump ahead (e.g. straight to Fixed) or cancel with a reason.
// Once it's Fixed, the tenant confirms it or reopens it ("still not fixed").
// Every change is saved as a timeline entry, and the tenant is emailed about the landlord's updates.
@Service
public class WorkOrderService {

    // Work orders the landlord can still move forward
    private static final Set<RequestStatus> ACTIVE = EnumSet.of(
            RequestStatus.APPROVED, RequestStatus.SCHEDULED, RequestStatus.IN_PROGRESS, RequestStatus.REOPENED);
    // Every status a work order can have (active or finished)
    private static final Set<RequestStatus> ALL = EnumSet.of(
            RequestStatus.APPROVED, RequestStatus.SCHEDULED, RequestStatus.IN_PROGRESS, RequestStatus.REOPENED,
            RequestStatus.RESOLVED, RequestStatus.CANCELLED);
    // Where the landlord can move an active work order
    private static final Set<RequestStatus> LANDLORD_TARGETS = EnumSet.of(
            RequestStatus.SCHEDULED, RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED, RequestStatus.CANCELLED);

    private static final Map<RequestStatus, String> LABELS = Map.of(
            RequestStatus.APPROVED, "Approved",
            RequestStatus.SCHEDULED, "Scheduled",
            RequestStatus.IN_PROGRESS, "In progress",
            RequestStatus.RESOLVED, "Fixed",
            RequestStatus.CANCELLED, "Cancelled");

    private final MaintenanceRequestRepository requestRepository;
    private final WorkOrderUpdateRepository updateRepository;
    private final AgentDraftRepository draftRepository;
    private final AppUserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public WorkOrderService(MaintenanceRequestRepository requestRepository, WorkOrderUpdateRepository updateRepository,
                            AgentDraftRepository draftRepository, AppUserRepository userRepository,
                            EmailService emailService, @Value("${app.frontend-url}") String frontendUrl) {
        this.requestRepository = requestRepository;
        this.updateRepository = updateRepository;
        this.draftRepository = draftRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    // --- Landlord ---

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> list(Long managerId) {
        return requestRepository.findByUnitPropertyManagerIdAndStatusInOrderByCreatedAtDesc(managerId, ALL).stream()
                .map(this::toResponse)
                .toList();
    }

    // Called when the manager approves the AI's draft: the first entry in the timeline
    @Transactional
    public void recordApproval(MaintenanceRequest request, Long managerId) {
        AppUser manager = userRepository.getReferenceById(managerId);
        updateRepository.save(new WorkOrderUpdate(request, RequestStatus.APPROVED, null, null, manager));
        emailTenant(request, RequestStatus.APPROVED, null);
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long managerId, Long requestId, RequestStatus newStatus,
                                          String note, LocalDate scheduledFor) {
        MaintenanceRequest request = requestRepository.findWithDetailsById(requestId)
                .filter(r -> r.getUnit().getProperty().getManager().getId().equals(managerId))
                .filter(r -> ALL.contains(r.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order not found"));
        if (!ACTIVE.contains(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This work order is already closed");
        }
        if (!LANDLORD_TARGETS.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A work order can't be moved to " + newStatus);
        }
        String cleanNote = blankToNull(note);

        switch (newStatus) {
            case SCHEDULED -> {
                if (scheduledFor == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick the date of the repair");
                }
                if (scheduledFor.isBefore(LocalDate.now())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The repair date can't be in the past");
                }
                request.setScheduledFor(scheduledFor);
            }
            case RESOLVED -> {
                request.setResolvedAt(Instant.now());
                request.setTenantConfirmedAt(null);
            }
            case CANCELLED -> {
                if (cleanNote == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tell the tenant why the job is cancelled");
                }
            }
            default -> { }
        }
        request.setStatus(newStatus);
        requestRepository.save(request);
        updateRepository.save(new WorkOrderUpdate(request, newStatus, cleanNote,
                newStatus == RequestStatus.SCHEDULED ? scheduledFor : null, userRepository.getReferenceById(managerId)));
        emailTenant(request, newStatus, cleanNote);
        return toResponse(request);
    }

    // --- Tenant ---

    @Transactional
    public void confirmFixed(Long tenantId, Long requestId) {
        MaintenanceRequest request = fixedRequestOf(tenantId, requestId);
        request.setTenantConfirmedAt(Instant.now());
        requestRepository.save(request);
        updateRepository.save(new WorkOrderUpdate(request, RequestStatus.RESOLVED, "Confirmed it's fixed", null,
                request.getTenant()));
    }

    @Transactional
    public void reopen(Long tenantId, Long requestId, String comment) {
        MaintenanceRequest request = fixedRequestOf(tenantId, requestId);
        if (blankToNull(comment) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tell your landlord what's still wrong");
        }
        request.setStatus(RequestStatus.REOPENED);
        request.setResolvedAt(null);
        requestRepository.save(request);
        updateRepository.save(new WorkOrderUpdate(request, RequestStatus.REOPENED, comment.trim(), null,
                request.getTenant()));
    }

    public List<WorkOrderUpdateResponse> timeline(Long requestId) {
        return updateRepository.findByRequestIdOrderByCreatedAt(requestId).stream()
                .map(WorkOrderUpdateResponse::from)
                .toList();
    }

    // --- Helpers ---

    // The tenant's own request, marked fixed and not yet confirmed
    private MaintenanceRequest fixedRequestOf(Long tenantId, Long requestId) {
        MaintenanceRequest request = requestRepository.findWithDetailsById(requestId)
                .filter(r -> r.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != RequestStatus.RESOLVED || request.getTenantConfirmedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request isn't waiting for your confirmation");
        }
        return request;
    }

    private void emailTenant(MaintenanceRequest request, RequestStatus status, String note) {
        AppUser tenant = request.getTenant();
        // Demo tenants have made-up addresses; only print their emails, never really send them
        if (tenant.isDemo() && emailService.sendsRealEmail()) {
            return;
        }
        StringBuilder text = new StringBuilder("Hi %s,%n%nThere's an update on your maintenance request \"%s\" (Unit %s):%n%n"
                .formatted(tenant.getFullName(), summaryOf(request), request.getUnit().getUnitNumber()));
        text.append("Status: ").append(LABELS.get(status)).append('\n');
        if (status == RequestStatus.SCHEDULED && request.getScheduledFor() != null) {
            text.append("Repair date: ").append(request.getScheduledFor()).append('\n');
        }
        if (note != null) {
            text.append("Note from your landlord: ").append(note).append('\n');
        }
        if (status == RequestStatus.RESOLVED) {
            text.append("\nPlease open LeasePilot to confirm it's fixed, or tell us if it isn't.\n");
        }
        text.append("\n").append(frontendUrl).append('\n');
        emailService.send(tenant.getEmail(), "Update on your maintenance request: " + LABELS.get(status), text.toString());
    }

    private WorkOrderResponse toResponse(MaintenanceRequest r) {
        AgentDraft draft = draftRepository.findByRequestId(r.getId()).orElse(null);
        return new WorkOrderResponse(r.getId(), r.getUnit().getUnitNumber(), r.getUnit().getProperty().getName(),
                r.getTenant().getFullName(), summaryOf(r), r.getCategory(), r.getUrgency(), r.getPhotoPath() != null,
                draft != null && draft.getVendor() != null ? draft.getVendor().getName() : null,
                draft != null && draft.getVendor() != null ? draft.getVendor().getPhone() : null,
                draft != null ? draft.getWorkOrderText() : null,
                r.getStatus(), r.getScheduledFor(), r.getTenantConfirmedAt(), r.getCreatedAt(), timeline(r.getId()));
    }

    private static String summaryOf(MaintenanceRequest r) {
        return r.getAiSummary() != null ? r.getAiSummary()
                : r.getDescription() != null ? r.getDescription() : "Maintenance request";
    }

    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
