package com.leasepilot.backend.model;

// Where the AI is with a new request. The AI runs in the background after the tenant submits.
// Null on old requests that were entered before this existed (e.g. the demo's past repairs).
public enum AiStatus {
    PROCESSING,
    READY,
    FAILED
}
