package com.leasepilot.backend.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.model.WorkOrderUpdate;

// One entry in a work order's timeline
public record WorkOrderUpdateResponse(
        RequestStatus status,
        String note,
        LocalDate scheduledFor,
        String authorName,
        Role authorRole,
        Instant createdAt) {

    public static WorkOrderUpdateResponse from(WorkOrderUpdate u) {
        return new WorkOrderUpdateResponse(u.getStatus(), u.getNote(), u.getScheduledFor(),
                u.getAuthor().getFullName(), u.getAuthor().getRole(), u.getCreatedAt());
    }
}
