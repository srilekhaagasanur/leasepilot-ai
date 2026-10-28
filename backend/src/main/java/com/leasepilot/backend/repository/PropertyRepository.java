package com.leasepilot.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.Property;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByManagerId(Long managerId);
}
