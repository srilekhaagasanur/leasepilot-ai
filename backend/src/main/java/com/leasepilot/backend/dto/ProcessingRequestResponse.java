package com.leasepilot.backend.dto;

import java.time.Instant;

import com.leasepilot.backend.model.AiStatus;
import com.leasepilot.backend.model.MaintenanceRequest;

// A request that doesn't have a draft yet: the AI is still working on it, or couldn't finish
public record ProcessingRequestResponse(
        Long requestId,
        String unitNumber,
        String propertyName,
        String tenantName,
        String description,
        boolean hasPhoto,
        boolean hasVoiceNote,
        Instant reportedAt,
        AiStatus aiStatus,
        // Why it failed (only when aiStatus is FAILED)
        String aiError) {

    public static ProcessingRequestResponse from(MaintenanceRequest r) {
        return new ProcessingRequestResponse(r.getId(), r.getUnit().getUnitNumber(), r.getUnit().getProperty().getName(),
                r.getTenant().getFullName(), r.getDescription(), r.getPhotoPath() != null, r.getVoiceNotePath() != null,
                r.getCreatedAt(), r.getAiStatus(), r.getAiError());
    }
}
