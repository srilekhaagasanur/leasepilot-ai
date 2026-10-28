package com.leasepilot.backend.model;

// Where a maintenance request is in its lifecycle
public enum RequestStatus {
    SUBMITTED,          // tenant sent it, AI hasn't finished yet
    AWAITING_APPROVAL,  // AI drafted a work order + reply, waiting for the manager
    APPROVED,           // manager approved; vendor dispatched
    REJECTED,           // manager rejected the AI's draft
    RESOLVED            // the repair is done
}
