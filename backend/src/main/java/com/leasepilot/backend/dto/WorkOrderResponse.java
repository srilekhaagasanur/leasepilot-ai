package com.leasepilot.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.Urgency;

// An approved request as the landlord tracks it: the job, its current status, and its timeline
public record WorkOrderResponse(
        Long requestId,
        String unitNumber,
        String propertyName,
        String tenantName,
        String summary,
        ServiceCategory category,
        Urgency urgency,
        boolean hasPhoto,
        String vendorName,
        String vendorPhone,
        String workOrderText,
        RequestStatus status,
        LocalDate scheduledFor,
        Instant tenantConfirmedAt,
        Instant reportedAt,
        List<WorkOrderUpdateResponse> updates) {
}
