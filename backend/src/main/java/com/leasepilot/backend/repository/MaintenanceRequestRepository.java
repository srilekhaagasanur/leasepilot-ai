package com.leasepilot.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.MaintenanceRequest;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    // A unit's repair history, newest first (used by the agent's "check past issues" tool)
    List<MaintenanceRequest> findByUnitIdOrderByCreatedAtDesc(Long unitId);

    List<MaintenanceRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
