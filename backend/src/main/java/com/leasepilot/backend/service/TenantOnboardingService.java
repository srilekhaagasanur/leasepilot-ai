package com.leasepilot.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.Lease;
import com.leasepilot.backend.model.LeaseClause;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.model.TenantInvite;
import com.leasepilot.backend.repository.LeaseClauseRepository;
import com.leasepilot.backend.repository.LeaseRepository;
import com.leasepilot.backend.repository.TenancyRepository;

// Connects a tenant account to the unit the owner invited them to, and gives them a lease.
// The AI agent reads the lease to decide who is responsible for a repair, so every tenant needs one.
// Until owners can upload their own lease, everyone gets these standard repair clauses.
@Service
public class TenantOnboardingService {

    private record Clause(String number, String title, String text) {}

    private static final List<Clause> STANDARD_CLAUSES = List.of(
            new Clause("7.1", "Landlord Repair Obligations",
                "Landlord shall keep the plumbing, electrical, heating, and cooling systems and all "
                + "landlord-supplied appliances in good working order, and shall make necessary repairs "
                + "within a reasonable time after receiving notice from Tenant."),
            new Clause("7.2", "Plumbing",
                "Landlord is responsible for repairing leaks, pipes, the water heater, and fixtures that fail "
                + "from normal wear and tear. Tenant is responsible for clogs caused by improper disposal, "
                + "including grease, wipes, or hygiene products."),
            new Clause("7.3", "Tenant-Caused Damage",
                "Tenant shall pay the reasonable cost of repairing any damage beyond normal wear and tear "
                + "caused by Tenant, household members, guests, or pets."),
            new Clause("7.4", "Appliances",
                "Landlord shall repair or replace landlord-supplied appliances (refrigerator, stove, "
                + "dishwasher, washer, dryer) unless the failure results from Tenant misuse. "
                + "Tenant-owned appliances are Tenant's responsibility."),
            new Clause("7.5", "Pest Control",
                "Landlord is responsible for pest treatment, except where an infestation results from "
                + "Tenant's failure to keep the unit reasonably clean."),
            new Clause("7.6", "Emergency Repairs and Access",
                "Emergencies include no heat when the outdoor temperature is below 50°F, a major water leak, "
                + "a gas smell, an electrical hazard, or no running water. Tenant must report emergencies "
                + "immediately. Landlord will respond within 24 hours and may enter the unit without "
                + "advance notice to address an emergency."),
            new Clause("7.7", "Tenant Minor Maintenance",
                "Tenant shall replace light bulbs and smoke detector batteries, replace HVAC filters every "
                + "three months, and keep drains free of obstructions.")
    );

    private final TenancyRepository tenancyRepository;
    private final LeaseRepository leaseRepository;
    private final LeaseClauseRepository clauseRepository;

    public TenantOnboardingService(TenancyRepository tenancyRepository, LeaseRepository leaseRepository,
                                   LeaseClauseRepository clauseRepository) {
        this.tenancyRepository = tenancyRepository;
        this.leaseRepository = leaseRepository;
        this.clauseRepository = clauseRepository;
    }

    @Transactional
    public void connect(TenantInvite invite, AppUser tenant) {
        Tenancy tenancy = new Tenancy(tenant, invite.getUnit());
        tenancy.approve();
        tenancyRepository.save(tenancy);

        Lease lease = leaseRepository.save(new Lease(invite.getUnit(), tenant,
                invite.getLeaseStart(), invite.getLeaseEnd(), invite.getMonthlyRent()));
        STANDARD_CLAUSES.forEach(c -> clauseRepository.save(new LeaseClause(lease, c.number(), c.title(), c.text())));

        invite.accept(tenant);
    }
}
