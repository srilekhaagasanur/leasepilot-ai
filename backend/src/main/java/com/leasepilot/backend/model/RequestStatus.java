package com.leasepilot.backend.model;

// Where a maintenance request is in its lifecycle.
// After approval it's a work order the landlord moves forward: Scheduled -> In progress -> Fixed.
public enum RequestStatus {
    SUBMITTED,          // tenant sent it, AI hasn't finished yet
    AWAITING_APPROVAL,  // AI drafted a work order + reply, waiting for the manager
    REJECTED,           // manager rejected the AI's draft
    APPROVED,           // manager approved; vendor contacted
    SCHEDULED,          // a repair date is set (see scheduledFor)
    IN_PROGRESS,        // the repair is underway
    RESOLVED,           // the landlord marked it fixed (the tenant can confirm or reopen)
    REOPENED,           // the tenant said it's still not fixed
    CANCELLED           // the landlord cancelled the job (with a reason)
}
