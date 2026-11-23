package com.leasepilot.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// A tenant the owner added to a unit, with their lease details.
// When someone signs up as a tenant with this email, they're connected to the unit automatically
// (see TenantOnboardingService). Until then the owner sees them as "Invited".
@Entity
@Table(name = "tenant_invites")
public class TenantInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private LocalDate leaseStart;

    @Column(nullable = false)
    private LocalDate leaseEnd;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // Set when the tenant signs up and gets connected (null while still invited)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private AppUser tenant;

    private Instant acceptedAt;

    protected TenantInvite() {
        // required by JPA
    }

    public TenantInvite(Unit unit, String fullName, String email, String phone,
                        LocalDate leaseStart, LocalDate leaseEnd, BigDecimal monthlyRent) {
        this.unit = unit;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.leaseStart = leaseStart;
        this.leaseEnd = leaseEnd;
        this.monthlyRent = monthlyRent;
    }

    public void accept(AppUser tenant) {
        this.tenant = tenant;
        this.acceptedAt = Instant.now();
    }

    public boolean isAccepted() { return acceptedAt != null; }

    public Long getId() { return id; }
    public Unit getUnit() { return unit; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getLeaseStart() { return leaseStart; }
    public LocalDate getLeaseEnd() { return leaseEnd; }
    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public Instant getCreatedAt() { return createdAt; }
    public AppUser getTenant() { return tenant; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
