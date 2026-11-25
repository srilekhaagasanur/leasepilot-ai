package com.leasepilot.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leasepilot.backend.dto.WorkOrderResponse;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.security.AppUserPrincipal;
import com.leasepilot.backend.service.WorkOrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The landlord's work orders: approved jobs and their progress (only managers can call these)
@RestController
@RequestMapping("/api/manager/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    // status: SCHEDULED (needs scheduledFor), IN_PROGRESS, RESOLVED (fixed), or CANCELLED (needs a note)
    public record StatusChange(
            @NotNull RequestStatus status,
            @Size(max = 2000) String note,
            LocalDate scheduledFor) {}

    // GET /api/manager/work-orders
    @GetMapping
    public List<WorkOrderResponse> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return workOrderService.list(me.id());
    }

    // POST /api/manager/work-orders/{requestId}/status   body: {"status": "SCHEDULED", "scheduledFor": "2026-12-01", "note": "..."}
    @PostMapping("/{requestId}/status")
    public WorkOrderResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long requestId,
                                          @Valid @RequestBody StatusChange body) {
        return workOrderService.updateStatus(me.id(), requestId, body.status(), body.note(), body.scheduledFor());
    }
}
