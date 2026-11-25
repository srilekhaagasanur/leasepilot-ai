package com.leasepilot.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.AiStatus;
import com.leasepilot.backend.model.MaintenanceRequest;
import com.leasepilot.backend.model.RequestStatus;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    // A unit's repair history, newest first (used by the agent's "check past issues" tool)
    List<MaintenanceRequest> findByUnitIdOrderByCreatedAtDesc(Long unitId);

    List<MaintenanceRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    // Loads the request together with its unit, property, and tenant. The background AI worker needs
    // these, and outside a web request they can't be loaded later on demand.
    @EntityGraph(attributePaths = {"unit", "unit.property", "tenant"})
    Optional<MaintenanceRequest> findWithDetailsById(Long id);

    // Requests in a manager's properties that the AI is still working on or couldn't finish
    @EntityGraph(attributePaths = {"unit", "unit.property", "tenant"})
    List<MaintenanceRequest> findByUnitPropertyManagerIdAndAiStatusInOrderByCreatedAtDesc(Long managerId,
                                                                                           Collection<AiStatus> statuses);

    List<MaintenanceRequest> findByAiStatus(AiStatus aiStatus);

    // Work orders (approved requests and later) in a manager's properties, newest first
    @EntityGraph(attributePaths = {"unit", "unit.property", "tenant"})
    List<MaintenanceRequest> findByUnitPropertyManagerIdAndStatusInOrderByCreatedAtDesc(Long managerId,
                                                                                         Collection<RequestStatus> statuses);
}
