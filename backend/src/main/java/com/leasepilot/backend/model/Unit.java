package com.leasepilot.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One apartment inside a property. The owner adds tenants to it (see TenantInvite).
@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String unitNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    protected Unit() {
        // required by JPA
    }

    public Unit(String unitNumber, Property property) {
        this.unitNumber = unitNumber;
        this.property = property;
    }

    public Long getId() { return id; }
    public String getUnitNumber() { return unitNumber; }
    public Property getProperty() { return property; }
}
