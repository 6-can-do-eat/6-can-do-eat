package com.team6.backend.address.domain.entity;

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
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail", nullable = false)
    private String detail;

    // 기본 배송지 여부
    @Column(name = "is_default")
    private boolean isDefault;

    public address(User user, String address, boolean isDefault) {
        this.user = user;
        this.address = address;
        this.isDefault = isDefault;
    }


}
