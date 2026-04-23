package com.team6.backend.address.domain.entity;

import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "p_address")
public class Address extends BaseEntity {

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

    public Address(AddressRequest request, User user) {
        this.address = request.getAddress();
        this.detail = request.getDetail();
        this.isDefault = false;
        this.user = user;
    }

    public void updateAddress(AddressRequest request) {
        this.address = request.getAddress();
        this.detail = request.getDetail();
        this.isDefault = request.isDefault();
    }

    // 어차피 값이 두개 밖에 없으니 dto갖고 오지 말자
    public void updateDefault() {
        this.isDefault = !this.isDefault;
    }

}
