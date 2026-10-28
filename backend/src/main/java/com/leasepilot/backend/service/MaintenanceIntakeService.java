package com.leasepilot.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.agent.MaintenanceAgent;
import com.leasepilot.backend.ai.IssueAnalysis;
import com.leasepilot.backend.ai.IssueAnalyzer;
import com.leasepilot.backend.ai.VoiceTranscribeClient;
import com.leasepilot.backend.dto.MaintenanceRequestResponse;
import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.TenancyRepository;

// Handles a new maintenance request from a tenant:
// save it -> store the photo/voice note -> transcribe the voice note -> let Muse Spark analyze it
// -> let the agent draft a work order and reply for the manager's approval queue.
@Service
public class MaintenanceIntakeService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceIntakeService.class);

    // Photo types Muse Spark accepts, mapped to the file extension we save them with
    private static final Map<String, String> PHOTO_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final TenancyRepository tenancyRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final AgentDraftRepository draftRepository;
    private final FileStorageService fileStorage;
    private final VoiceTranscribeClient voiceTranscribeClient;
    private final IssueAnalyzer issueAnalyzer;
    private final MaintenanceAgent maintenanceAgent;

    public MaintenanceIntakeService(TenancyRepository tenancyRepository,
                                    MaintenanceRequestRepository requestRepository,
                                    AgentDraftRepository draftRepository,
                                    FileStorageService fileStorage,
                                    VoiceTranscribeClient voiceTranscribeClient,
                                    IssueAnalyzer issueAnalyzer,
                                    MaintenanceAgent maintenanceAgent) {
        this.tenancyRepository = tenancyRepository;
        this.requestRepository = requestRepository;
        this.draftRepository = draftRepository;
        this.fileStorage = fileStorage;
        this.voiceTranscribeClient = voiceTranscribeClient;
        this.issueAnalyzer = issueAnalyzer;
        this.maintenanceAgent = maintenanceAgent;
    }

    // A tenant's own requests, newest first. The reply is shown only once the manager approved it.
    public List<MaintenanceRequestResponse> listForTenant(Long tenantId) {
        return requestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(r -> MaintenanceRequestResponse.from(r, null, draftRepository.findByRequestId(r.getId())
                        .filter(d -> d.getApprovalStatus() == ApprovalStatus.APPROVED)
                        .map(AgentDraft::getTenantReplyText)
                        .orElse(null)))
                .toList();
    }

    public MaintenanceRequestResponse submit(Long tenantId, String description,
                                             MultipartFile photo, MultipartFile voiceNote) {
        // 1. Check the input before doing anything
        boolean hasText = description != null && !description.isBlank();
        boolean hasPhoto = photo != null && !photo.isEmpty();
        boolean hasVoice = voiceNote != null && !voiceNote.isEmpty();
        if (!hasText && !hasVoice) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Describe the problem in text or a voice note");
        }
        String photoType = hasPhoto ? photo.getContentType() : null;
        if (hasPhoto && !PHOTO_TYPES.containsKey(photoType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Photo must be JPEG, PNG, WebP, or GIF");
        }
        if (hasVoice && !isWav(voiceNote)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Voice note must be a WAV file (mono, 16-bit, 16 kHz or 24 kHz)");
        }

        // 2. Only tenants approved for a unit can submit requests
        Tenancy tenancy = tenancyRepository.findFirstByTenantIdAndStatus(tenantId, ApprovalStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This tenant isn't approved for any unit yet"));

        // 3. Save the request first, so nothing is lost even if an AI step fails
        MaintenanceRequest request = requestRepository.save(
                new MaintenanceRequest(tenancy.getUnit(), tenancy.getTenant(), hasText ? description.trim() : null));

        byte[] photoBytes = hasPhoto ? bytesOf(photo) : null;
        byte[] voiceBytes = hasVoice ? bytesOf(voiceNote) : null;
        if (hasPhoto) {
            request.setPhotoPath(fileStorage.saveForRequest(request.getId(), "photo", PHOTO_TYPES.get(photoType), photoBytes));
        }
        if (hasVoice) {
            request.setVoiceNotePath(fileStorage.saveForRequest(request.getId(), "voice", "wav", voiceBytes));
        }
        requestRepository.save(request);

        // 4. AI steps. If one fails, keep the request and report the error instead of losing it.
        String aiError = null;
        try {
            if (hasVoice) {
                request.setTranscript(voiceTranscribeClient.transcribe(voiceBytes));
            }
            IssueAnalysis analysis = issueAnalyzer.analyze(request.getDescription(), request.getTranscript(),
                    photoBytes, photoType);
            request.setAiSummary(analysis.summary());
            request.setCategory(analysis.category());
            request.setUrgency(analysis.urgency());
            request.setUrgencyReason(analysis.urgencyReason());
            request.setPhotoObservations(blankToNull(analysis.photoObservations()));
            requestRepository.save(request);
        } catch (RuntimeException e) {
            log.error("AI analysis failed for request {}", request.getId(), e);
            aiError = "AI analysis failed: " + e.getMessage();
            requestRepository.save(request);
            return MaintenanceRequestResponse.from(request, aiError, null);
        }

        // 5. The agent checks the lease, history, and vendors, and drafts a work order + reply
        try {
            maintenanceAgent.run(request);
        } catch (RuntimeException e) {
            log.error("Agent failed for request {}", request.getId(), e);
            aiError = "Agent failed: " + e.getMessage();
        }

        return MaintenanceRequestResponse.from(request, aiError, null);
    }

    private static boolean isWav(MultipartFile file) {
        String type = file.getContentType();
        String name = file.getOriginalFilename();
        return "audio/wav".equals(type) || "audio/x-wav".equals(type) || "audio/wave".equals(type)
                || (name != null && name.toLowerCase().endsWith(".wav"));
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload", e);
        }
    }

    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }
}
