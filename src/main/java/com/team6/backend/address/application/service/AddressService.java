package com.team6.backend.address.application.service;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.userInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final userInfoRepository userInfoRepository;
    private final SecurityUtils securityUtils;

    // post 배송지 등록(costomor)
    @Transactional
    public AddressResponse addAddress(AddressRequest request, User user)  {
        // 1.  @AuthenticationPrincipal controller에서 넘겨줌.
        // TODO: 이미 컨트롤러에서 검증된 User 객체를 받아도  findById
        // 2. 찾은 후에 배송지 등록 -> address entity 저장

        Address address = addressRepository.save(new Address(request, user));
        return new AddressResponse(address);
    }


    @Transactional
    public void deleteAddress(UUID adId) {
        Address address = findById(adId);
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse updateAddress(UUID adId, AddressRequest request) {
        Address address = findById(adId);
        address.updateAddress(request);
        return new AddressResponse(address);
    }

    @Transactional
    public AddressResponse UpdateDefault(UUID adId) {
        Address address = findById(adId);
        address.updateDefault();
        return new AddressResponse(address);
    }

    // TODO: 권한.....봐야겠지..

    // TODO: 에러코드 수정하기
    private Address findById(UUID adId) {
        return addressRepository.findById(adId).
                orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }
}
