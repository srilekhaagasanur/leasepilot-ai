package com.leasepilot.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.dto.MaintenanceRequestResponse;
import com.leasepilot.backend.model.AgentDraft;
import com.leasepilot.backend.model.AiStatus;
import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.TenancyRepository;

// Handles a new maintenance request from a tenant: check it, save it and its photo/voice note,
// then hand it to RequestProcessor, which runs the AI steps in the background.
// The tenant gets an answer right away instead of waiting for the AI.
@Service
public class MaintenanceIntakeService {

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
    private final RequestProcessor requestProcessor;
    private final WorkOrderService workOrderService;

    public MaintenanceIntakeService(TenancyRepository tenancyRepository,
                                    MaintenanceRequestRepository requestRepository,
                                    AgentDraftRepository draftRepository,
                                    FileStorageService fileStorage,
                                    RequestProcessor requestProcessor,
                                    WorkOrderService workOrderService) {
        this.tenancyRepository = tenancyRepository;
        this.requestRepository = requestRepository;
        this.draftRepository = draftRepository;
        this.fileStorage = fileStorage;
        this.requestProcessor = requestProcessor;
        this.workOrderService = workOrderService;
    }

    // A tenant's own requests, newest first, with each work order's timeline.
    // The reply is shown only once the manager approved it.
    public List<MaintenanceRequestResponse> listForTenant(Long tenantId) {
        return requestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(r -> MaintenanceRequestResponse.from(r, draftRepository.findByRequestId(r.getId())
                        .filter(d -> d.getApprovalStatus() == ApprovalStatus.APPROVED)
                        .map(AgentDraft::getTenantReplyText)
                        .orElse(null), workOrderService.timeline(r.getId())))
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

        // 3. Save the request and its files, then let the AI work on it in the background
        MaintenanceRequest request = new MaintenanceRequest(tenancy.getUnit(), tenancy.getTenant(),
                hasText ? description.trim() : null);
        request.setAiStatus(AiStatus.PROCESSING);
        requestRepository.save(request);

        if (hasPhoto) {
            request.setPhotoPath(fileStorage.saveForRequest(request.getId(), "photo", PHOTO_TYPES.get(photoType), bytesOf(photo)));
        }
        if (hasVoice) {
            request.setVoiceNotePath(fileStorage.saveForRequest(request.getId(), "voice", "wav", bytesOf(voiceNote)));
        }
        requestRepository.save(request);

        requestProcessor.processInBackground(request.getId());
        return MaintenanceRequestResponse.from(request, null, List.of());
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
}
