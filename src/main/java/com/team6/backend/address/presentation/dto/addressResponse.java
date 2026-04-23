package com.team6.backend.address.presentation.dto;

import com.team6.backend.address.domain.entity.address;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
public class addressResponse {

    private final UUID adId;
    private final String address;
    private final String detail;
    private final boolean isDefault;

    // 엔티티를 받아서 필드에 직접 매핑하는 생성자
    public addressResponse(address addressEntity) {
        this.adId = addressEntity.getAdId(); // 엔티티의 ID 필드명에 맞춰 수정하세요
        this.address = addressEntity.getAddress();
        this.detail = addressEntity.getDetail();
        this.isDefault = addressEntity.isDefault();
    }

}
