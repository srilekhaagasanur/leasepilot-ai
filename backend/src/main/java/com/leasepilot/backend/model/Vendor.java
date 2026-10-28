package com.leasepilot.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A repair company the manager works with (plumber, electrician, ...)
@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    protected Vendor() {
        // required by JPA
    }

    public Vendor(String name, ServiceCategory category, String phone, String email) {
        this.name = name;
        this.category = category;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ServiceCategory getCategory() { return category; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
