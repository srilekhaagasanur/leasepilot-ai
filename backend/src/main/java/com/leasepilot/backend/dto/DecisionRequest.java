package com.leasepilot.backend.dto;

import jakarta.validation.constraints.NotNull;

// Body for approve/reject calls. No login yet, so the frontend says which manager is acting.
public record DecisionRequest(
        @NotNull Long managerId,
        String note) {
}
