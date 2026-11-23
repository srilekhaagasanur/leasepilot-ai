package com.leasepilot.backend.dto;

// Optional body for approve/reject calls: the manager's note
public record DecisionRequest(String note) {
}
