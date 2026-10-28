package com.leasepilot.backend.agent;

// One tool call the agent made: which tool, with what arguments, and a short summary of the result.
// Shown to the manager as the agent's "work log".
public record ToolStep(String tool, String arguments, String resultSummary) {
}
