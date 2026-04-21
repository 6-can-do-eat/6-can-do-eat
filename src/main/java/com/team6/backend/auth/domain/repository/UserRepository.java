package com.team6.backend.auth.domain.repository;

import com.team6.backend.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
    boolean existsByNickname(String nickname);
    Optional<User> findByUsername(String username);

}
