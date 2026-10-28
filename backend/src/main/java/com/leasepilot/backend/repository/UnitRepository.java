package com.leasepilot.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.Unit;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByInviteCode(String inviteCode);

    List<Unit> findByPropertyId(Long propertyId);
}
