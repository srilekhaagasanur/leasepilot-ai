package com.leasepilot.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.Lease;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.Property;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.model.Unit;
import com.leasepilot.backend.repository.AgentDraftRepository;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.LeaseClauseRepository;
import com.leasepilot.backend.repository.LeaseRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.PasswordResetTokenRepository;
import com.leasepilot.backend.repository.PropertyRepository;
import com.leasepilot.backend.repository.TenancyRepository;
import com.leasepilot.backend.repository.TenantInviteRepository;
import com.leasepilot.backend.repository.UnitRepository;
import com.leasepilot.backend.seed.DemoDataSeeder;

// Puts the demo back to its starting point, so every demo run starts clean.
// Only the demo manager's (Dana's) properties are touched: they're deleted completely, together with
// their tenants' accounts, requests, drafts, and files, and Oakwood Apartments is rebuilt.
// Other owners' data is never affected.
@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    private final AppUserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenancyRepository tenancyRepository;
    private final TenantInviteRepository inviteRepository;
    private final LeaseRepository leaseRepository;
    private final LeaseClauseRepository clauseRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final AgentDraftRepository draftRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final FileStorageService fileStorage;
    private final DemoDataSeeder seeder;

    public DemoResetService(AppUserRepository userRepository, PropertyRepository propertyRepository,
                            UnitRepository unitRepository, TenancyRepository tenancyRepository,
                            TenantInviteRepository inviteRepository, LeaseRepository leaseRepository,
                            LeaseClauseRepository clauseRepository, MaintenanceRequestRepository requestRepository,
                            AgentDraftRepository draftRepository, PasswordResetTokenRepository resetTokenRepository,
                            FileStorageService fileStorage,
                            DemoDataSeeder seeder) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.tenancyRepository = tenancyRepository;
        this.inviteRepository = inviteRepository;
        this.leaseRepository = leaseRepository;
        this.clauseRepository = clauseRepository;
        this.requestRepository = requestRepository;
        this.draftRepository = draftRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.fileStorage = fileStorage;
        this.seeder = seeder;
    }

    @Transactional
    public void reset() {
        AppUser manager = userRepository.findByEmail(DemoDataSeeder.DEMO_MANAGER_EMAIL)
                .orElseThrow(() -> new IllegalStateException("Demo manager is missing"));

        List<Long> requestIds = new ArrayList<>();
        for (Property property : propertyRepository.findByManagerId(manager.getId())) {
            deleteProperty(property, requestIds);
        }
        // Deletes run in a batch at the end of the transaction; send them now so the
        // rebuilt demo users don't clash with the old ones' emails
        userRepository.flush();

        seeder.seedDemoProperty(manager);
        requestIds.forEach(fileStorage::deleteRequestFiles);
        log.info("Demo reset: demo properties rebuilt");
    }

    // Children before parents, so no row is left pointing at something deleted
    private void deleteProperty(Property property, List<Long> requestIds) {
        List<AppUser> tenants = new ArrayList<>();
        for (Unit unit : unitRepository.findByPropertyId(property.getId())) {
            for (MaintenanceRequest request : requestRepository.findByUnitIdOrderByCreatedAtDesc(unit.getId())) {
                draftRepository.findByRequestId(request.getId()).ifPresent(draftRepository::delete);
                requestIds.add(request.getId());
                requestRepository.delete(request);
            }
            inviteRepository.deleteAll(inviteRepository.findByUnitIdOrderByCreatedAt(unit.getId()));
            for (Tenancy tenancy : tenancyRepository.findByUnitId(unit.getId())) {
                tenants.add(tenancy.getTenant());
                tenancyRepository.delete(tenancy);
            }
            for (Lease lease : leaseRepository.findByUnitId(unit.getId())) {
                clauseRepository.deleteAll(clauseRepository.findByLeaseIdOrderByClauseNumber(lease.getId()));
                leaseRepository.delete(lease);
            }
            unitRepository.delete(unit);
        }
        propertyRepository.delete(property);
        // The tenants' accounts only existed for this property (each tenant has one unit).
        // Someone who signed up as Jordan may have asked for a password reset link; remove those first.
        tenants.forEach(t -> resetTokenRepository.deleteAll(resetTokenRepository.findByUserId(t.getId())));
        userRepository.deleteAll(tenants);
    }
}
