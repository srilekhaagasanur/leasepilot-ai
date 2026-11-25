package com.leasepilot.backend.model;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

// One step in a work order's timeline: who moved it to which status, when, and their note.
// Both the landlord and the tenant see these.
@Entity
@Table(name = "work_order_updates")
public class WorkOrderUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private MaintenanceRequest request;

    // The status the request moved to
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    // A message for the other side, e.g. "Plumber coming between 10 and 12"
    @Column(columnDefinition = "TEXT")
    private String note;

    // Set when the update scheduled the repair
    private LocalDate scheduledFor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected WorkOrderUpdate() {
        // required by JPA
    }

    public WorkOrderUpdate(MaintenanceRequest request, RequestStatus status, String note,
                           LocalDate scheduledFor, AppUser author) {
        this.request = request;
        this.status = status;
        this.note = note;
        this.scheduledFor = scheduledFor;
        this.author = author;
    }

    public Long getId() { return id; }
    public MaintenanceRequest getRequest() { return request; }
    public RequestStatus getStatus() { return status; }
    public String getNote() { return note; }
    public LocalDate getScheduledFor() { return scheduledFor; }
    public AppUser getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
}
