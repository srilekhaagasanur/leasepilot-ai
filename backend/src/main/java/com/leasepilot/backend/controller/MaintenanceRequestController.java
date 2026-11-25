package com.leasepilot.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.leasepilot.backend.dto.MaintenanceRequestResponse;
import com.leasepilot.backend.security.AppUserPrincipal;
import com.leasepilot.backend.service.MaintenanceIntakeService;
import com.leasepilot.backend.service.WorkOrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/tenant/requests")
public class MaintenanceRequestController {

    private final MaintenanceIntakeService intakeService;
    private final WorkOrderService workOrderService;

    public MaintenanceRequestController(MaintenanceIntakeService intakeService, WorkOrderService workOrderService) {
        this.intakeService = intakeService;
        this.workOrderService = workOrderService;
    }

    public record ReopenRequest(@Size(max = 2000) String comment) {}

    // POST /api/tenant/requests (multipart form), as the logged-in tenant
    //   description - optional text
    //   photo       - optional image file
    //   voiceNote   - optional WAV file
    // Needs at least a description or a voice note.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRequestResponse submit(
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam(required = false) String description,
            @RequestPart(required = false) MultipartFile photo,
            @RequestPart(required = false) MultipartFile voiceNote) {
        return intakeService.submit(me.id(), description, photo, voiceNote);
    }

    // GET /api/tenant/requests -> the logged-in tenant's requests, newest first
    @GetMapping
    public List<MaintenanceRequestResponse> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return intakeService.listForTenant(me.id());
    }

    // POST /api/tenant/requests/{id}/confirm-fixed  -> the tenant agrees the landlord's "Fixed"
    @PostMapping("/{requestId}/confirm-fixed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmFixed(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long requestId) {
        workOrderService.confirmFixed(me.id(), requestId);
    }

    // POST /api/tenant/requests/{id}/reopen   body: {"comment": "still dripping"}
    @PostMapping("/{requestId}/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long requestId,
                       @Valid @RequestBody ReopenRequest body) {
        workOrderService.reopen(me.id(), requestId, body.comment());
    }
}
