package com.team6.backend.review.domain.repository;

import com.team6.backend.review.domain.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    boolean existsByOrder_Id(UUID orderId);

    @EntityGraph(attributePaths = {"user", "store", "order"})
    Page<ReviewEntity> findByStore_IdAndDeletedAtIsNull(UUID storeId, Pageable pageable);
}
