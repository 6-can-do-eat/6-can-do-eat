package com.team6.backend.address.domain.repository;

import com.team6.backend.address.domain.entity.address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface addressRepository extends JpaRepository<address, UUID> {
}
