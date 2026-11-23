package com.leasepilot.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.leasepilot.backend.agent.MaintenanceAgent;
import com.leasepilot.backend.ai.IssueAnalysis;
import com.leasepilot.backend.ai.IssueAnalyzer;
import com.leasepilot.backend.ai.SamClient;
import com.leasepilot.backend.ai.VoiceTranscribeClient;
import com.leasepilot.backend.model.AiStatus;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;

// Runs the AI steps for a request in the background, so the tenant doesn't wait 30+ seconds:
// transcribe the voice note -> Muse Spark analyzes the report -> SAM 3.1 finds the damage in the photo
// -> the agent drafts a work order and reply for the manager's approval queue.
//
// Each step is skipped if it already succeeded, so a retry after a failure only redoes (and only
// pays for) what's missing.
@Service
public class RequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(RequestProcessor.class);

    private final MaintenanceRequestRepository requestRepository;
    private final FileStorageService fileStorage;
    private final VoiceTranscribeClient voiceTranscribeClient;
    private final IssueAnalyzer issueAnalyzer;
    private final SamClient samClient;
    private final MaintenanceAgent maintenanceAgent;

    public RequestProcessor(MaintenanceRequestRepository requestRepository, FileStorageService fileStorage,
                            VoiceTranscribeClient voiceTranscribeClient, IssueAnalyzer issueAnalyzer,
                            SamClient samClient, MaintenanceAgent maintenanceAgent) {
        this.requestRepository = requestRepository;
        this.fileStorage = fileStorage;
        this.voiceTranscribeClient = voiceTranscribeClient;
        this.issueAnalyzer = issueAnalyzer;
        this.samClient = samClient;
        this.maintenanceAgent = maintenanceAgent;
    }

    // @Async: returns immediately; the work runs on a background thread from Spring's task pool.
    // The request must already be saved (with aiStatus PROCESSING) before this is called.
    @Async
    public void processInBackground(Long requestId) {
        MaintenanceRequest request = requestRepository.findWithDetailsById(requestId).orElse(null);
        if (request == null) {
            return;
        }
        try {
            runMissingSteps(request);
            request.setAiStatus(AiStatus.READY);
            request.setAiError(null);
            log.info("Request {}: draft ready for manager approval", requestId);
        } catch (RuntimeException e) {
            log.error("Request {}: AI processing failed", requestId, e);
            request.setAiStatus(AiStatus.FAILED);
            request.setAiError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        try {
            requestRepository.save(request);
        } catch (RuntimeException e) {
            // e.g. the demo was reset while the AI was working, so the request no longer exists
            log.warn("Request {}: couldn't save the AI result ({})", requestId, e.getMessage());
        }
    }

    private void runMissingSteps(MaintenanceRequest request) {
        Long id = request.getId();
        byte[] photoBytes = request.getPhotoPath() != null ? fileStorage.read(request.getPhotoPath()) : null;
        String photoType = request.getPhotoPath() != null ? FileStorageService.photoTypeOf(request.getPhotoPath()) : null;

        // 1. Voice note -> text
        if (request.getVoiceNotePath() != null && request.getTranscript() == null) {
            log.info("Request {}: Muse Voice Transcribe -> transcribing voice note", id);
            request.setTranscript(voiceTranscribeClient.transcribe(fileStorage.read(request.getVoiceNotePath())));
            log.info("Request {}: transcript = \"{}\"", id, request.getTranscript());
            requestRepository.save(request);
        }

        // 2. Summary, category, urgency, and what's visible in the photo
        if (request.getAiSummary() == null) {
            log.info("Request {}: Muse Spark -> analyzing report{}", id, photoBytes != null ? " and photo" : "");
            IssueAnalysis analysis = issueAnalyzer.analyze(request.getDescription(), request.getTranscript(),
                    photoBytes, photoType);
            log.info("Request {}: analysis = {}", id, analysis);
            request.setAiSummary(analysis.summary());
            request.setCategory(analysis.category());
            request.setUrgency(analysis.urgency());
            request.setUrgencyReason(analysis.urgencyReason());
            request.setPhotoObservations(blankToNull(analysis.photoObservations()));
            request.setDamagePhrase(blankToNull(analysis.damagePhrase()));
            requestRepository.save(request);
        }

        // 3. SAM 3.1 outlines the damage. Nice to have: if it fails, carry on without it.
        if (photoBytes != null && request.getDamagePhrase() != null && request.getDamageSegmentation() == null) {
            try {
                log.info("Request {}: SAM 3.1 -> segmenting \"{}\" in photo", id, request.getDamagePhrase());
                request.setDamageSegmentation(samClient.segment(photoBytes, photoType, request.getDamagePhrase()));
                log.info("Request {}: SAM 3.1 {}", id,
                        request.getDamageSegmentation() == null ? "found no match" : "found the damage");
                requestRepository.save(request);
            } catch (RuntimeException e) {
                log.warn("Request {}: SAM segmentation failed", id, e);
            }
        }

        // 4. The agent checks the lease, history, and vendors, and drafts a work order + reply
        log.info("Request {}: Muse Spark agent -> drafting work order", id);
        maintenanceAgent.run(request);
    }

    // Background work doesn't survive a restart. Anything still marked PROCESSING when the app starts
    // was interrupted, so mark it failed; the manager can press Retry.
    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedRequests() {
        for (MaintenanceRequest request : requestRepository.findByAiStatus(AiStatus.PROCESSING)) {
            request.setAiStatus(AiStatus.FAILED);
            request.setAiError("Interrupted by a server restart");
            requestRepository.save(request);
            log.warn("Request {}: marked as failed (interrupted by a restart)", request.getId());
        }
    }

    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }
}
