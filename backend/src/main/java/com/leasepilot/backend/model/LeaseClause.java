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

// One section of a lease, e.g. "7.2 Plumbing Repairs". The agent cites these in its drafts.
@Entity
@Table(name = "lease_clauses")
public class LeaseClause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lease_id")
    private Lease lease;

    // The section number as printed in the lease, e.g. "7.2"
    @Column(nullable = false)
    private String clauseNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    protected LeaseClause() {
        // required by JPA
    }

    public LeaseClause(Lease lease, String clauseNumber, String title, String text) {
        this.lease = lease;
        this.clauseNumber = clauseNumber;
        this.title = title;
        this.text = text;
    }

    public Long getId() { return id; }
    public Lease getLease() { return lease; }
    public String getClauseNumber() { return clauseNumber; }
    public String getTitle() { return title; }
    public String getText() { return text; }
}
