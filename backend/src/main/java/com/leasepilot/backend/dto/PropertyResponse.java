package com.leasepilot.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// An owner's property with its units and the tenants added to each unit
public record PropertyResponse(Long id, String name, String address, List<UnitResponse> units) {

    public record UnitResponse(Long id, String unitNumber, List<TenantResponse> tenants) {}

    // signedUp is false while the tenant hasn't created their account yet ("Invited")
    public record TenantResponse(
            Long inviteId,
            String fullName,
            String email,
            String phone,
            LocalDate leaseStart,
            LocalDate leaseEnd,
            BigDecimal monthlyRent,
            boolean signedUp) {}
}
