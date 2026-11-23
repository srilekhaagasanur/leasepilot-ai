package com.leasepilot.backend.dto;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.model.Tenancy;

// Who a user is, as the frontend needs it: the logged-in user (/api/auth/me) and the demo accounts
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        // For tenants: their unit and whether they've been linked to it (null for managers)
        String unitNumber,
        ApprovalStatus tenancyStatus,
        boolean demo) {

    public static UserResponse from(AppUser user, Tenancy tenancy) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                tenancy != null ? tenancy.getUnit().getUnitNumber() : null,
                tenancy != null ? tenancy.getStatus() : null,
                user.isDemo());
    }
}
