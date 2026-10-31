package com.leasepilot.backend.dto;

import java.time.Instant;

import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.Urgency;

// What the API sends back about a maintenance request
public record MaintenanceRequestResponse(
        Long id,
        String unitNumber,
        String tenantName,
        String description,
        boolean hasPhoto,
        boolean hasVoiceNote,
        String transcript,
        String aiSummary,
        ServiceCategory category,
        Urgency urgency,
        String urgencyReason,
        String photoObservations,
        String damagePhrase,
        // SAM 3.1's box + mask text for the damage in the photo (null if none)
        String damageSegmentation,
        RequestStatus status,
        Instant createdAt,
        // Set only if an AI step failed; the request is still saved
        String aiError,
        // The manager-approved reply to the tenant (null until approved)
        String replyFromManagement) {

    public static MaintenanceRequestResponse from(MaintenanceRequest r, String aiError, String replyFromManagement) {
        return new MaintenanceRequestResponse(
                r.getId(),
                r.getUnit().getUnitNumber(),
                r.getTenant().getFullName(),
                r.getDescription(),
                r.getPhotoPath() != null,
                r.getVoiceNotePath() != null,
                r.getTranscript(),
                r.getAiSummary(),
                r.getCategory(),
                r.getUrgency(),
                r.getUrgencyReason(),
                r.getPhotoObservations(),
                r.getDamagePhrase(),
                r.getDamageSegmentation(),
                r.getStatus(),
                r.getCreatedAt(),
                aiError,
                replyFromManagement);
    }
}
