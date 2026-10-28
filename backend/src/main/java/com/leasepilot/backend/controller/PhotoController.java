package com.leasepilot.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.service.FileStorageService;

// Serves the photo a tenant attached to a request, so the dashboard can show it
@RestController
public class PhotoController {

    private final MaintenanceRequestRepository requestRepository;
    private final FileStorageService fileStorage;

    public PhotoController(MaintenanceRequestRepository requestRepository, FileStorageService fileStorage) {
        this.requestRepository = requestRepository;
        this.fileStorage = fileStorage;
    }

    // GET /api/requests/{id}/photo
    @GetMapping("/api/requests/{requestId}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long requestId) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .filter(r -> r.getPhotoPath() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No photo for this request"));
        return ResponseEntity.ok()
                .contentType(mediaTypeFor(request.getPhotoPath()))
                .body(fileStorage.read(request.getPhotoPath()));
    }

    private static MediaType mediaTypeFor(String path) {
        if (path.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (path.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (path.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
