package com.leasepilot.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.Unit;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    boolean existsByPropertyIdAndUnitNumberIgnoreCase(Long propertyId, String unitNumber);

    List<Unit> findByPropertyId(Long propertyId);
}
