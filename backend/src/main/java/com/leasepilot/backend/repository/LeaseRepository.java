package com.leasepilot.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.Lease;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    Optional<Lease> findFirstByUnitIdOrderByStartDateDesc(Long unitId);
}
