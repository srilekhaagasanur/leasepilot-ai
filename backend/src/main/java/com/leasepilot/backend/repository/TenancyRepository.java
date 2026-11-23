package com.leasepilot.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.Tenancy;

public interface TenancyRepository extends JpaRepository<Tenancy, Long> {


    List<Tenancy> findByTenantId(Long tenantId);

    // The unit a tenant is approved to live in (only approved tenants can submit requests)
    Optional<Tenancy> findFirstByTenantIdAndStatus(Long tenantId, ApprovalStatus status);

    List<Tenancy> findByUnitId(Long unitId);
}
