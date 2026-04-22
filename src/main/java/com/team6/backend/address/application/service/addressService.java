package com.team6.backend.address.application.service;

import com.team6.backend.address.domain.entity.address;
import com.team6.backend.address.domain.repository.addressRepository;
import com.team6.backend.address.presentation.dto.addressRequest;
import com.team6.backend.address.presentation.dto.addressResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.repository.userInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class addressService {

    private final addressRepository addressRepository;
    private final userInfoRepository userInfoRepository;
    private final SecurityUtils securityUtils;

    // post 배송지 등록(costomor)
    @Transactional
    public addressResponse addAddress(addressRequest request) {
        // 1. security에서 userID 찾기
        UUID userId = securityUtils.getCurrentUserId();
        userInfoRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 2. 찾은 후에 배송지 등록 -> address entity 저장
        // 유저가 입력.
        address address = new address(userId, request.getAddress());

    }



}
