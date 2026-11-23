package com.leasepilot.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Saves uploaded files to the uploads folder on disk.
// We always pick the file name ourselves (never use the uploader's name),
// so nobody can sneak in a path like "../../etc/passwd".
@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    // Saves bytes under uploads/requests/{requestId}/ and returns the path relative to uploads/
    public String saveForRequest(Long requestId, String kind, String extension, byte[] bytes) {
        Path folder = uploadRoot.resolve("requests").resolve(requestId.toString());
        String fileName = kind + "-" + UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(folder);
            Files.write(folder.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save upload", e);
        }
        return uploadRoot.relativize(folder.resolve(fileName)).toString();
    }

    public byte[] read(String relativePath) {
        Path file = uploadRoot.resolve(relativePath).normalize();
        if (!file.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Path is outside the uploads folder");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload", e);
        }
    }

    // The image type of a saved photo, from the extension we saved it with (see MaintenanceIntakeService)
    public static String photoTypeOf(String path) {
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    // Deletes one request's photo and voice note (used by the demo reset)
    public void deleteRequestFiles(Long requestId) {
        Path folder = uploadRoot.resolve("requests").resolve(requestId.toString());
        if (!Files.exists(folder)) {
            return;
        }
        // Deepest paths first, so each folder is already empty when we delete it
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete uploads", e);
        }
    }
}
