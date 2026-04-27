package com.team6.backend.review.domain.repository;

import com.team6.backend.review.domain.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    boolean existsByOrder_Id(UUID orderId);

    @EntityGraph(attributePaths = {"user", "store", "order"})
    Page<ReviewEntity> findByStore_StoreIdAndDeletedAtIsNull(UUID storeId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ReviewEntity r WHERE r.store.storeId = :storeId AND r.deletedAt IS NULL")
    Double calculateAverageRatingByStoreId(@Param("storeId") UUID storeId);

}
