package com.leasepilot.backend.dto;

import java.time.Instant;

import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.Tenancy;

// A tenant's request to join a unit, as shown to the manager
public record TenancyResponse(
        Long tenancyId,
        Long tenantId,
        String tenantName,
        String tenantEmail,
        String unitNumber,
        String propertyName,
        ApprovalStatus status,
        Instant requestedAt) {

    public static TenancyResponse from(Tenancy t) {
        return new TenancyResponse(
                t.getId(), t.getTenant().getId(), t.getTenant().getFullName(), t.getTenant().getEmail(),
                t.getUnit().getUnitNumber(), t.getUnit().getProperty().getName(),
                t.getStatus(), t.getRequestedAt());
    }
}
