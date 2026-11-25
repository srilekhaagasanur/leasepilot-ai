package com.leasepilot.backend.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.Property;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.TenantInvite;
import com.leasepilot.backend.model.Unit;
import com.leasepilot.backend.model.Urgency;
import com.leasepilot.backend.model.Vendor;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.PropertyRepository;
import com.leasepilot.backend.repository.TenantInviteRepository;
import com.leasepilot.backend.repository.UnitRepository;
import com.leasepilot.backend.repository.VendorRepository;
import com.leasepilot.backend.service.TenantOnboardingService;

// Fills an empty database with demo data when the app starts.
// Does nothing if any users already exist, so restarts don't create duplicates.
// The demo property is built the same way a real owner would: units, then tenants added by email.
// All names, addresses, and phone numbers are fictional.
@Component
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_MANAGER_EMAIL = "dana.whitfield@example.com";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppUserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantInviteRepository inviteRepository;
    private final VendorRepository vendorRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final TenantOnboardingService onboarding;

    public DemoDataSeeder(AppUserRepository userRepository, PropertyRepository propertyRepository,
                          UnitRepository unitRepository, TenantInviteRepository inviteRepository,
                          VendorRepository vendorRepository, MaintenanceRequestRepository requestRepository,
                          TenantOnboardingService onboarding) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.inviteRepository = inviteRepository;
        this.vendorRepository = vendorRepository;
        this.requestRepository = requestRepository;
        this.onboarding = onboarding;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already present, skipping seed");
            return;
        }

        // --- Vendors, one per category (shared by every property) ---
        vendorRepository.save(new Vendor("ClearFlow Plumbing", ServiceCategory.PLUMBING, "(614) 555-0142", "dispatch@clearflow.example.com"));
        vendorRepository.save(new Vendor("BrightSpark Electric", ServiceCategory.ELECTRICAL, "(614) 555-0187", "jobs@brightspark.example.com"));
        vendorRepository.save(new Vendor("Comfort Air HVAC", ServiceCategory.HVAC, "(614) 555-0123", "service@comfortair.example.com"));
        vendorRepository.save(new Vendor("ApplianceCare Repair", ServiceCategory.APPLIANCE, "(614) 555-0168", "repairs@appliancecare.example.com"));
        vendorRepository.save(new Vendor("GreenShield Pest Control", ServiceCategory.PEST_CONTROL, "(614) 555-0119", "help@greenshield.example.com"));
        vendorRepository.save(new Vendor("HandyPro General Repairs", ServiceCategory.GENERAL, "(614) 555-0155", "office@handypro.example.com"));

        // --- The demo property manager ---
        AppUser dana = new AppUser(DEMO_MANAGER_EMAIL, "Dana Whitfield", Role.MANAGER);
        dana.markAsDemo();
        seedDemoProperty(userRepository.save(dana));

        log.info("Seeded demo data: 6 vendors, Dana's Oakwood Apartments with 4 units, 3 tenants, 1 open invite");
    }

    // Oakwood Apartments with its tenants and past repairs. Also used by the demo reset.
    public void seedDemoProperty(AppUser manager) {
        Property oakwood = propertyRepository.save(
                new Property("Oakwood Apartments", "1420 Oakwood Ave, Columbus, OH 43201", manager));
        Unit unit1A = unitRepository.save(new Unit("1A", oakwood));
        Unit unit2B = unitRepository.save(new Unit("2B", oakwood));
        Unit unit3C = unitRepository.save(new Unit("3C", oakwood));
        Unit unit4D = unitRepository.save(new Unit("4D", oakwood));

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        AppUser marcus = demoTenant(unit1A, "Marcus Reed", "marcus.reed@example.com", "(614) 555-0131", start, end, "1350.00");
        AppUser priya = demoTenant(unit2B, "Priya Shah", "priya.shah@example.com", "(614) 555-0132", start, end, "1475.00");
        demoTenant(unit3C, "Elena Novak", "elena.novak@example.com", "(614) 555-0133", start, end, "1425.00");

        // Jordan was added by the owner but hasn't signed up yet.
        // Sign up as a tenant with this email to see the account get connected to 4D.
        inviteRepository.save(new TenantInvite(unit4D, "Jordan Kim", "jordan.kim@example.com", "(614) 555-0134",
                LocalDate.of(2026, 12, 1), LocalDate.of(2027, 11, 30), new BigDecimal("1395.00")));

        // --- Past repairs. Unit 2B has a kitchen sink that keeps leaking, so the agent
        //     has a pattern to notice when Priya reports it again. ---
        pastRequest(unit2B, priya, "Water is leaking under the kitchen sink and the cabinet floor is wet.",
                "Leak under kitchen sink", ServiceCategory.PLUMBING, Urgency.MEDIUM, "2026-03-14", "2026-03-16");
        pastRequest(unit2B, priya, "Kitchen sink is leaking again, same spot as in March. Put a bucket under it.",
                "Repeat leak under kitchen sink", ServiceCategory.PLUMBING, Urgency.HIGH, "2026-06-02", "2026-06-03");
        pastRequest(unit2B, priya, "Bedroom outlet sparked when I plugged in a lamp.",
                "Sparking bedroom outlet", ServiceCategory.ELECTRICAL, Urgency.HIGH, "2026-08-20", "2026-08-20");
        pastRequest(unit1A, marcus, "AC is running but the apartment isn't cooling down.",
                "AC not cooling", ServiceCategory.HVAC, Urgency.MEDIUM, "2026-07-09", "2026-07-11");
    }

    // A tenant the owner invited who has already signed up. Demo tenants have no password;
    // they log in with the "Try as..." buttons.
    private AppUser demoTenant(Unit unit, String fullName, String email, String phone,
                               LocalDate start, LocalDate end, String rent) {
        TenantInvite invite = inviteRepository.save(
                new TenantInvite(unit, fullName, email, phone, start, end, new BigDecimal(rent)));
        AppUser tenant = new AppUser(email, fullName, Role.TENANT);
        tenant.markAsDemo();
        userRepository.save(tenant);
        onboarding.connect(invite, tenant);
        return tenant;
    }

    private void pastRequest(Unit unit, AppUser tenant, String description, String summary,
                             ServiceCategory category, Urgency urgency, String reportedOn, String resolvedOn) {
        MaintenanceRequest request = new MaintenanceRequest(unit, tenant, description);
        request.setAiSummary(summary);
        request.setCategory(category);
        request.setUrgency(urgency);
        request.setStatus(RequestStatus.RESOLVED);
        request.setCreatedAt(Instant.parse(reportedOn + "T14:00:00Z"));
        request.setResolvedAt(Instant.parse(resolvedOn + "T20:00:00Z"));
        // Old, closed repairs: the tenant already confirmed them
        request.setTenantConfirmedAt(Instant.parse(resolvedOn + "T20:00:00Z"));
        requestRepository.save(request);
    }
}
