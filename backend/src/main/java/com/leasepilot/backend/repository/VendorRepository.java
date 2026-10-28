package com.leasepilot.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.ServiceCategory;
import com.leasepilot.backend.model.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    List<Vendor> findByCategory(ServiceCategory category);
}
