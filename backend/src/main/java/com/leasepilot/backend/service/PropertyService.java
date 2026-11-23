package com.leasepilot.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.dto.PropertyResponse;
import com.leasepilot.backend.dto.PropertyResponse.TenantResponse;
import com.leasepilot.backend.dto.PropertyResponse.UnitResponse;
import com.leasepilot.backend.model.Property;
import com.leasepilot.backend.model.TenantInvite;
import com.leasepilot.backend.model.Unit;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.PropertyRepository;
import com.leasepilot.backend.repository.TenantInviteRepository;
import com.leasepilot.backend.repository.UnitRepository;

// An owner's buildings: add properties, units, and tenants.
// Every method checks that the property belongs to the logged-in owner.
// The changing methods return the owner's full, updated list, so the page can simply redraw it.
@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantInviteRepository inviteRepository;
    private final AppUserRepository userRepository;

    public PropertyService(PropertyRepository propertyRepository, UnitRepository unitRepository,
                           TenantInviteRepository inviteRepository, AppUserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list(Long managerId) {
        return propertyRepository.findByManagerId(managerId).stream()
                .sorted(Comparator.comparing(Property::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<PropertyResponse> addProperty(Long managerId, String name, String address) {
        propertyRepository.save(new Property(name.trim(), address.trim(), userRepository.getReferenceById(managerId)));
        return list(managerId);
    }

    @Transactional
    public List<PropertyResponse> addUnit(Long managerId, Long propertyId, String unitNumber) {
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> p.getManager().getId().equals(managerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        String number = unitNumber.trim();
        if (unitRepository.existsByPropertyIdAndUnitNumberIgnoreCase(propertyId, number)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This property already has a unit " + number);
        }
        unitRepository.save(new Unit(number, property));
        return list(managerId);
    }

    @Transactional
    public List<PropertyResponse> addTenant(Long managerId, Long unitId, String fullName, String email, String phone,
                                            LocalDate leaseStart, LocalDate leaseEnd, BigDecimal monthlyRent) {
        Unit unit = ownedUnit(managerId, unitId);
        if (!leaseEnd.isAfter(leaseStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The lease must end after it starts");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email already has a LeasePilot account");
        }
        if (inviteRepository.existsByEmailAndAcceptedAtIsNull(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email has already been added to a unit");
        }
        inviteRepository.save(new TenantInvite(unit, fullName.trim(), normalizedEmail, blankToNull(phone),
                leaseStart, leaseEnd, monthlyRent));
        return list(managerId);
    }

    // Removes a tenant the owner added by mistake, as long as they haven't signed up yet
    @Transactional
    public List<PropertyResponse> cancelInvite(Long managerId, Long inviteId) {
        TenantInvite invite = inviteRepository.findById(inviteId)
                .filter(i -> i.getUnit().getProperty().getManager().getId().equals(managerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        if (invite.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This tenant has already signed up");
        }
        inviteRepository.delete(invite);
        return list(managerId);
    }

    private Unit ownedUnit(Long managerId, Long unitId) {
        return unitRepository.findById(unitId)
                .filter(u -> u.getProperty().getManager().getId().equals(managerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));
    }

    private PropertyResponse toResponse(Property property) {
        List<UnitResponse> units = unitRepository.findByPropertyId(property.getId()).stream()
                .sorted(Comparator.comparing(Unit::getUnitNumber, String.CASE_INSENSITIVE_ORDER))
                .map(unit -> new UnitResponse(unit.getId(), unit.getUnitNumber(),
                        inviteRepository.findByUnitIdOrderByCreatedAt(unit.getId()).stream()
                                .map(i -> new TenantResponse(i.getId(), i.getFullName(), i.getEmail(), i.getPhone(),
                                        i.getLeaseStart(), i.getLeaseEnd(), i.getMonthlyRent(), i.isAccepted()))
                                .toList()))
                .toList();
        return new PropertyResponse(property.getId(), property.getName(), property.getAddress(), units);
    }

    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
