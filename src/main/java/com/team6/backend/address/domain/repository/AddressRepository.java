package com.team6.backend.address.domain.repository;

import com.team6.backend.address.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}
