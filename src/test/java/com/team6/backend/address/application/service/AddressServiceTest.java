package com.team6.backend.address.application.service;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private Address address;
    private UUID userId;
    private UUID adId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adId = UUID.randomUUID();
        
        // 가짜(Mock) 객체 대신 실제 User 객체를 생성하여 ID 반환 문제 해결
        user = User.builder()
                .id(userId)
                .username("testUser")
                .role(Role.CUSTOMER)
                .build();

        AddressRequest request = new AddressRequest("서울시 강남구", "101호", false, "집");
        address = new Address(request, user);
    }

    @Test
    @DisplayName("배송지 등록 성공")
    void addAddress_Success() {
        // given
        AddressRequest request = new AddressRequest("서울시 강남구", "101호", false, "집");
        given(addressRepository.save(any(Address.class))).willReturn(address);

        // when
        AddressResponse response = addressService.addAddress(request, user);

        // then
        assertThat(response).isNotNull();
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("배송지 목록 조회 성공")
    void getAddress_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Address> addressPage = new PageImpl<>(Collections.singletonList(address));
        given(addressRepository.findByUserId(userId, pageable)).willReturn(addressPage);

        // when
        Page<AddressResponse> result = addressService.getAddress(user, null, 0, 10, "createdAt", false);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("배송지 상세 조회 성공 (본인)")
    void getAddressById_Success() {
        // given
        given(addressRepository.findById(adId)).willReturn(Optional.of(address));
        given(securityUtils.getCurrentUserId()).willReturn(userId);
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when
        AddressResponse response = addressService.getAddressById(adId);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("배송지 삭제 성공 (소프트 딜리트)")
    void deleteAddress_Success() {
        // given
        given(addressRepository.findById(adId)).willReturn(Optional.of(address));
        given(securityUtils.getCurrentUserId()).willReturn(userId);
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when
        addressService.deleteAddress(adId);

        // then
        assertThat(address.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("배송지 상세 조회 실패 - 권한 없음")
    void getAddressById_Fail_Forbidden() {
        // given
        UUID otherUserId = UUID.randomUUID();
        given(addressRepository.findById(adId)).willReturn(Optional.of(address));
        given(securityUtils.getCurrentUserId()).willReturn(otherUserId);
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> addressService.getAddressById(adId))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(CommonErrorCode.FORBIDDEN.getMessage());
    }
}
