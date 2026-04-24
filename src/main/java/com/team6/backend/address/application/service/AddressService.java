package com.team6.backend.address.application.service;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.exception.ErrorCode;
import com.team6.backend.global.infrastructure.exception.StoreErrorCode;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.userInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    // TODO: 로그인 한 유저랑 현 주소에 잇는 유저랑 같은 지 확인 로직 필요(서비스)
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

    // TODO: 유저를....확인해야 하는지 고민
    // 검색 필수 조건.
    @Transactional(readOnly = true)
    public Page<AddressResponse> getAddress(User user, String alias, int page, int size, String sortByTime, boolean isAsc) {
        Sort.Direction sortDirection = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, sortByTime);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 검색 조건에 따라 다르게
        // 검색은 alias(별칭)
        Page<Address> addressList;
        if(alias != null && !alias.isEmpty()){
            addressList =addressRepository.findByUserIdAndAliasContainingIgnoreCase(user.getId(),alias,pageable);
        } else {
            addressList = addressRepository.findByUserId(user.getId(), pageable);
        }
        return addressList.map(AddressResponse::new);
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID adId) {
        Address address = findById(adId);
        return new AddressResponse(address);
    }

    // TODO: 소프트니?
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
