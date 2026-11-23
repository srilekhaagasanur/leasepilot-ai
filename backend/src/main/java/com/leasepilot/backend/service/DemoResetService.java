package com.leasepilot.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.TenancyRepository;
import com.leasepilot.backend.seed.DemoDataSeeder;

// Puts the demo back to its starting point, so every demo run (or judge's visit) starts clean.
// People, the building, leases, and vendors never change during a demo, so they're kept.
// Everything the demo creates or changes is reset: requests, drafts, uploaded files, and Jordan's join request.
@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    private final AgentDraftRepository draftRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final TenancyRepository tenancyRepository;
    private final AppUserRepository userRepository;
    private final FileStorageService fileStorage;
    private final DemoDataSeeder seeder;

    public DemoResetService(AgentDraftRepository draftRepository, MaintenanceRequestRepository requestRepository,
                            TenancyRepository tenancyRepository, AppUserRepository userRepository,
                            FileStorageService fileStorage, DemoDataSeeder seeder) {
        this.draftRepository = draftRepository;
        this.requestRepository = requestRepository;
        this.tenancyRepository = tenancyRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.seeder = seeder;
    }

    @Transactional
    public void reset() {
        // Drafts point at requests, so they go first
        draftRepository.deleteAllInBatch();
        requestRepository.deleteAllInBatch();
        seeder.seedPastRequests();

        // Jordan starts out waiting for the manager to approve the join request
        userRepository.findByEmail("jordan.kim@example.com").ifPresent(jordan ->
                tenancyRepository.findByTenantId(jordan.getId()).forEach(t -> t.resetToPending()));

        fileStorage.deleteAllRequestFiles();
        log.info("Demo reset: requests, drafts, and uploads cleared; past requests restored");
    }
}
