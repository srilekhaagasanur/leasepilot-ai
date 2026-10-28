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
import com.leasepilot.backend.model.Lease;
import com.leasepilot.backend.model.LeaseClause;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.Property;
import com.leasepilot.backend.model.RequestStatus;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.model.Unit;
import com.leasepilot.backend.model.Urgency;
import com.leasepilot.backend.model.Vendor;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.LeaseClauseRepository;
import com.leasepilot.backend.repository.LeaseRepository;
import com.leasepilot.backend.repository.MaintenanceRequestRepository;
import com.leasepilot.backend.repository.PropertyRepository;
import com.leasepilot.backend.repository.TenancyRepository;
import com.leasepilot.backend.repository.UnitRepository;
import com.leasepilot.backend.repository.VendorRepository;

// Fills an empty database with demo data when the app starts.
// Does nothing if any users already exist, so restarts don't create duplicates.
// All names, addresses, and phone numbers are fictional.
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppUserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenancyRepository tenancyRepository;
    private final LeaseRepository leaseRepository;
    private final LeaseClauseRepository clauseRepository;
    private final VendorRepository vendorRepository;
    private final MaintenanceRequestRepository requestRepository;

    public DemoDataSeeder(AppUserRepository userRepository, PropertyRepository propertyRepository,
                          UnitRepository unitRepository, TenancyRepository tenancyRepository,
                          LeaseRepository leaseRepository, LeaseClauseRepository clauseRepository,
                          VendorRepository vendorRepository, MaintenanceRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.unitRepository = unitRepository;
        this.tenancyRepository = tenancyRepository;
        this.leaseRepository = leaseRepository;
        this.clauseRepository = clauseRepository;
        this.vendorRepository = vendorRepository;
        this.requestRepository = requestRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already present, skipping seed");
            return;
        }

        // --- People ---
        AppUser manager = userRepository.save(new AppUser("dana.whitfield@example.com", "Dana Whitfield", Role.MANAGER));
        AppUser marcus = userRepository.save(new AppUser("marcus.reed@example.com", "Marcus Reed", Role.TENANT));
        AppUser priya = userRepository.save(new AppUser("priya.shah@example.com", "Priya Shah", Role.TENANT));
        AppUser elena = userRepository.save(new AppUser("elena.novak@example.com", "Elena Novak", Role.TENANT));
        AppUser jordan = userRepository.save(new AppUser("jordan.kim@example.com", "Jordan Kim", Role.TENANT));

        // --- Building and units ---
        Property oakwood = propertyRepository.save(
                new Property("Oakwood Apartments", "1420 Oakwood Ave, Columbus, OH 43201", manager));
        Unit unit1A = unitRepository.save(new Unit("1A", oakwood, "OAK-1A-4H2M"));
        Unit unit2B = unitRepository.save(new Unit("2B", oakwood, "OAK-2B-7F3K"));
        Unit unit3C = unitRepository.save(new Unit("3C", oakwood, "OAK-3C-9P6R"));
        Unit unit4D = unitRepository.save(new Unit("4D", oakwood, "OAK-4D-2X8T"));

        // --- Who lives where (Jordan's request for 4D is still waiting for approval) ---
        approvedTenancy(marcus, unit1A);
        approvedTenancy(priya, unit2B);
        approvedTenancy(elena, unit3C);
        tenancyRepository.save(new Tenancy(jordan, unit4D));

        // --- Leases, each with the same standard clauses ---
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        addLeaseWithClauses(unit1A, marcus, start, end, new BigDecimal("1350.00"));
        addLeaseWithClauses(unit2B, priya, start, end, new BigDecimal("1475.00"));
        addLeaseWithClauses(unit3C, elena, start, end, new BigDecimal("1425.00"));

        // --- Vendors, one per category ---
        vendorRepository.save(new Vendor("ClearFlow Plumbing", ServiceCategory.PLUMBING, "(614) 555-0142", "dispatch@clearflow.example.com"));
        vendorRepository.save(new Vendor("BrightSpark Electric", ServiceCategory.ELECTRICAL, "(614) 555-0187", "jobs@brightspark.example.com"));
        vendorRepository.save(new Vendor("Comfort Air HVAC", ServiceCategory.HVAC, "(614) 555-0123", "service@comfortair.example.com"));
        vendorRepository.save(new Vendor("ApplianceCare Repair", ServiceCategory.APPLIANCE, "(614) 555-0168", "repairs@appliancecare.example.com"));
        vendorRepository.save(new Vendor("GreenShield Pest Control", ServiceCategory.PEST_CONTROL, "(614) 555-0119", "help@greenshield.example.com"));
        vendorRepository.save(new Vendor("HandyPro General Repairs", ServiceCategory.GENERAL, "(614) 555-0155", "office@handypro.example.com"));

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

        log.info("Seeded demo data: 5 users, 1 property, 4 units, 3 leases, 6 vendors, 4 past requests");
    }

    private void approvedTenancy(AppUser tenant, Unit unit) {
        Tenancy tenancy = new Tenancy(tenant, unit);
        tenancy.approve();
        tenancyRepository.save(tenancy);
    }

    private void addLeaseWithClauses(Unit unit, AppUser tenant, LocalDate start, LocalDate end, BigDecimal rent) {
        Lease lease = leaseRepository.save(new Lease(unit, tenant, start, end, rent));

        clauseRepository.save(new LeaseClause(lease, "7.1", "Landlord Repair Obligations",
                "Landlord shall keep the plumbing, electrical, heating, and cooling systems and all "
                + "landlord-supplied appliances in good working order, and shall make necessary repairs "
                + "within a reasonable time after receiving notice from Tenant."));
        clauseRepository.save(new LeaseClause(lease, "7.2", "Plumbing",
                "Landlord is responsible for repairing leaks, pipes, the water heater, and fixtures that fail "
                + "from normal wear and tear. Tenant is responsible for clogs caused by improper disposal, "
                + "including grease, wipes, or hygiene products."));
        clauseRepository.save(new LeaseClause(lease, "7.3", "Tenant-Caused Damage",
                "Tenant shall pay the reasonable cost of repairing any damage beyond normal wear and tear "
                + "caused by Tenant, household members, guests, or pets."));
        clauseRepository.save(new LeaseClause(lease, "7.4", "Appliances",
                "Landlord shall repair or replace landlord-supplied appliances (refrigerator, stove, "
                + "dishwasher, washer, dryer) unless the failure results from Tenant misuse. "
                + "Tenant-owned appliances are Tenant's responsibility."));
        clauseRepository.save(new LeaseClause(lease, "7.5", "Pest Control",
                "Landlord is responsible for pest treatment, except where an infestation results from "
                + "Tenant's failure to keep the unit reasonably clean."));
        clauseRepository.save(new LeaseClause(lease, "7.6", "Emergency Repairs and Access",
                "Emergencies include no heat when the outdoor temperature is below 50°F, a major water leak, "
                + "a gas smell, an electrical hazard, or no running water. Tenant must report emergencies "
                + "immediately. Landlord will respond within 24 hours and may enter the unit without "
                + "advance notice to address an emergency."));
        clauseRepository.save(new LeaseClause(lease, "7.7", "Tenant Minor Maintenance",
                "Tenant shall replace light bulbs and smoke detector batteries, replace HVAC filters every "
                + "three months, and keep drains free of obstructions."));
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
        requestRepository.save(request);
    }
}
