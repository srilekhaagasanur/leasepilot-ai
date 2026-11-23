package com.leasepilot.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Links a tenant to a unit. Starts as PENDING until the property's manager approves it.
@Entity
@Table(name = "tenancies")
public class Tenancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private AppUser tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    // When the manager approved or rejected it (null while pending)
    private Instant decidedAt;

    protected Tenancy() {
        // required by JPA
    }

    public Tenancy(AppUser tenant, Unit unit) {
        this.tenant = tenant;
        this.unit = unit;
    }

    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.decidedAt = Instant.now();
    }

    public void reject() {
        this.status = ApprovalStatus.REJECTED;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getTenant() { return tenant; }
    public Unit getUnit() { return unit; }
    public ApprovalStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
