package com.leasepilot.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A person using the app: either a property manager (owner) or a tenant.
// Demo users (from the seed data) have no password; they log in with the "Try as..." buttons.
// Named AppUser (table app_users) because "user" is a reserved word in some databases.
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // BCrypt hash of the password, never the password itself. Null for demo users.
    private String passwordHash;

    // Demo users can be logged into without a password (see AuthController.demoLogin)
    @Column(nullable = false)
    private boolean demo = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
        // required by JPA
    }

    public AppUser(String email, String fullName, Role role) {
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isDemo() { return demo; }
    public Instant getCreatedAt() { return createdAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void markAsDemo() { this.demo = true; }
}
