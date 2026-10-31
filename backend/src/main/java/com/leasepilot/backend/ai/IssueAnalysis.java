package com.leasepilot.backend.ai;

import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.Urgency;

// What Muse Spark works out from a tenant's report (text, transcript, and photo)
public record IssueAnalysis(
        String summary,
        ServiceCategory category,
        Urgency urgency,
        String urgencyReason,
        String photoObservations,
        // Short noun phrase naming the visible damage, used as the SAM 3.1 prompt ("" if none)
        String damagePhrase) {
}
