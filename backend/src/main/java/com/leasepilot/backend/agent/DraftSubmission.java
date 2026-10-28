package com.leasepilot.backend.agent;

import com.leasepilot.backend.model.Responsibility;

// The arguments of the agent's final submit_draft tool call
public record DraftSubmission(
        Responsibility responsibility,
        String citedClauseNumber,   // "" if no clause applies
        long vendorId,              // 0 if no vendor should be sent
        String workOrderText,
        String tenantReplyText,
        String reasoning) {
}
