package com.team6.backend.area.domain.repository;

import com.team6.backend.area.domain.entity.Area;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {

    Page<Area> findByNameContainingIgnoreCase(String name, Pageable pageable);

}