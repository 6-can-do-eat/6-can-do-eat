package com.team6.backend.menu.domain.repository;

import com.team6.backend.menu.domain.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID>, MenuRepositoryCustom {
}
