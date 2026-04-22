package com.team6.backend.address.domain.entity;

import com.team6.backend.address.presentation.dto.addressRequest;
import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "p_address")
public class address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "address_id")
    private UUID adId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail", nullable = false)
    private String detail;

    // 기본 배송지 여부
    @Column(name = "is_default")
    private boolean isDefault;

    public address(addressRequest request, User user) {
        this.address = request.getAddress();
        this.detail = request.getDetail();
        this.isDefault = request.isDefault();
        this.user = user;
    }

}
