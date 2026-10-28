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

// A problem a tenant reported. The tenant fills in the first fields; the AI fills in the rest.
// Old requests for a unit double as that unit's repair history.
@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private AppUser tenant;

    // --- What the tenant sent ---

    @Column(columnDefinition = "TEXT")
    private String description;

    // Where the uploaded files are stored on disk (null if none was sent)
    private String photoPath;
    private String voiceNotePath;

    // --- What the AI worked out ---

    // Text of the voice note, from Muse Voice Transcribe
    @Column(columnDefinition = "TEXT")
    private String transcript;

    // One-line summary of the issue, from Muse Spark
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    // Why the AI chose that urgency, in one sentence
    @Column(columnDefinition = "TEXT")
    private String urgencyReason;

    // What the AI saw in the photo that matters for the repair
    @Column(columnDefinition = "TEXT")
    private String photoObservations;

    // --- Lifecycle ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.SUBMITTED;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant resolvedAt;

    protected MaintenanceRequest() {
        // required by JPA
    }

    public MaintenanceRequest(Unit unit, AppUser tenant, String description) {
        this.unit = unit;
        this.tenant = tenant;
        this.description = description;
    }

    public Long getId() { return id; }
    public Unit getUnit() { return unit; }
    public AppUser getTenant() { return tenant; }
    public String getDescription() { return description; }
    public String getPhotoPath() { return photoPath; }
    public String getVoiceNotePath() { return voiceNotePath; }
    public String getTranscript() { return transcript; }
    public String getAiSummary() { return aiSummary; }
    public ServiceCategory getCategory() { return category; }
    public Urgency getUrgency() { return urgency; }
    public String getUrgencyReason() { return urgencyReason; }
    public String getPhotoObservations() { return photoObservations; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setVoiceNotePath(String voiceNotePath) { this.voiceNotePath = voiceNotePath; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public void setCategory(ServiceCategory category) { this.category = category; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public void setUrgencyReason(String urgencyReason) { this.urgencyReason = urgencyReason; }
    public void setPhotoObservations(String photoObservations) { this.photoObservations = photoObservations; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public void markResolved() {
        this.status = RequestStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }
}
