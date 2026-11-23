package com.leasepilot.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leasepilot.backend.dto.PropertyResponse;
import com.leasepilot.backend.security.AppUserPrincipal;
import com.leasepilot.backend.service.PropertyService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// The owner's properties, units, and tenants (only managers can call these, see SecurityConfig)
@RestController
@RequestMapping("/api/manager")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    public record NewProperty(@NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 255) String address) {}

    public record NewUnit(@NotBlank @Size(max = 20) String unitNumber) {}

    public record NewTenant(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @NotNull LocalDate leaseStart,
            @NotNull LocalDate leaseEnd,
            @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal monthlyRent) {}

    // GET /api/manager/properties
    @GetMapping("/properties")
    public List<PropertyResponse> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return propertyService.list(me.id());
    }

    // POST /api/manager/properties   body: {"name": "...", "address": "..."}
    @PostMapping("/properties")
    public List<PropertyResponse> addProperty(@AuthenticationPrincipal AppUserPrincipal me,
                                              @Valid @RequestBody NewProperty body) {
        return propertyService.addProperty(me.id(), body.name(), body.address());
    }

    // POST /api/manager/properties/{id}/units   body: {"unitNumber": "1A"}
    @PostMapping("/properties/{propertyId}/units")
    public List<PropertyResponse> addUnit(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long propertyId,
                                          @Valid @RequestBody NewUnit body) {
        return propertyService.addUnit(me.id(), propertyId, body.unitNumber());
    }

    // POST /api/manager/units/{id}/tenants   body: {"fullName", "email", "phone", "leaseStart", "leaseEnd", "monthlyRent"}
    @PostMapping("/units/{unitId}/tenants")
    public List<PropertyResponse> addTenant(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long unitId,
                                            @Valid @RequestBody NewTenant body) {
        return propertyService.addTenant(me.id(), unitId, body.fullName(), body.email(), body.phone(),
                body.leaseStart(), body.leaseEnd(), body.monthlyRent());
    }

    // DELETE /api/manager/invites/{id}  -> remove a tenant who hasn't signed up yet
    @DeleteMapping("/invites/{inviteId}")
    public List<PropertyResponse> cancelInvite(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long inviteId) {
        return propertyService.cancelInvite(me.id(), inviteId);
    }
}
