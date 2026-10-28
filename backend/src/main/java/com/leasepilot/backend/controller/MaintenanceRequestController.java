package com.leasepilot.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.leasepilot.backend.dto.MaintenanceRequestResponse;
import com.leasepilot.backend.service.MaintenanceIntakeService;

@RestController
@RequestMapping("/api/tenant/requests")
public class MaintenanceRequestController {

    private final MaintenanceIntakeService intakeService;

    public MaintenanceRequestController(MaintenanceIntakeService intakeService) {
        this.intakeService = intakeService;
    }

    // POST /api/tenant/requests (multipart form)
    //   tenantId    - which demo tenant is submitting (no login, so the frontend sends it)
    //   description - optional text
    //   photo       - optional image file
    //   voiceNote   - optional WAV file
    // Needs at least a description or a voice note.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRequestResponse submit(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String description,
            @RequestPart(required = false) MultipartFile photo,
            @RequestPart(required = false) MultipartFile voiceNote) {
        return intakeService.submit(tenantId, description, photo, voiceNote);
    }

    // GET /api/tenant/requests?tenantId=3 -> this tenant's requests, newest first
    @GetMapping
    public List<MaintenanceRequestResponse> list(@RequestParam Long tenantId) {
        return intakeService.listForTenant(tenantId);
    }
}
