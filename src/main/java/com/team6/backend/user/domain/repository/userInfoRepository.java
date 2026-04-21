package com.team6.backend.user.domain.repository;

import com.team6.backend.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface userInfoRepository extends JpaRepository<User, UUID> {
}
