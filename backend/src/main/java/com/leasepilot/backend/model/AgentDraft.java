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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// What the AI agent proposes for a request. Sits in the manager's approval queue;
// nothing is sent to the tenant or vendor until the manager approves it.
@Entity
@Table(name = "agent_drafts")
public class AgentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", unique = true)
    private MaintenanceRequest request;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String workOrderText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tenantReplyText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Responsibility responsibility;

    // The lease clause the agent based its decision on (null if none applied)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cited_clause_id")
    private LeaseClause citedClause;

    // The vendor the agent picked (null if the tenant is responsible)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    // The agent's explanation of its decision, shown to the manager
    @Column(columnDefinition = "TEXT")
    private String reasoning;

    // JSON list of the tool calls the agent made, so the manager can see how it decided
    @Column(columnDefinition = "TEXT")
    private String toolTrace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    // Optional comment the manager leaves when approving or rejecting
    @Column(columnDefinition = "TEXT")
    private String managerNote;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant decidedAt;

    protected AgentDraft() {
        // required by JPA
    }

    public AgentDraft(MaintenanceRequest request, String workOrderText, String tenantReplyText,
                      Responsibility responsibility, LeaseClause citedClause, Vendor vendor, String reasoning) {
        this.request = request;
        this.workOrderText = workOrderText;
        this.tenantReplyText = tenantReplyText;
        this.responsibility = responsibility;
        this.citedClause = citedClause;
        this.vendor = vendor;
        this.reasoning = reasoning;
    }

    public void approve(String note) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.managerNote = note;
        this.decidedAt = Instant.now();
    }

    public void reject(String note) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.managerNote = note;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public MaintenanceRequest getRequest() { return request; }
    public String getWorkOrderText() { return workOrderText; }
    public String getTenantReplyText() { return tenantReplyText; }
    public Responsibility getResponsibility() { return responsibility; }
    public LeaseClause getCitedClause() { return citedClause; }
    public Vendor getVendor() { return vendor; }
    public String getReasoning() { return reasoning; }
    public String getToolTrace() { return toolTrace; }
    public void setToolTrace(String toolTrace) { this.toolTrace = toolTrace; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public String getManagerNote() { return managerNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
