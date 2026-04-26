package com.team6.backend.address.application.service;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.userInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
@TestPropertySource(properties = {

})
class AddressServiceIntegrationTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private userInfoRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 유저 생성 및 저장
        testUser = User.builder()
                .username("testuser_" + UUID.randomUUID().toString().substring(0, 8))
                .password("password")
                .role(Role.CUSTOMER)
                .build();
        testUser = userRepository.save(testUser);

        // 2. SecurityContext에 인증 정보 설정 (권한 정보 포함)
        // SecurityUtils.getCurrentUserRole()이 "ROLE_CUSTOMER"를 인식할 수 있도록 설정
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + testUser.getRole().name()));
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(testUser.getId(), null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("실제 운영 DB에 배송지 저장 및 조회 통합 테스트")
    void addAndGetAddress_RealDB_Test() {
        // given
        AddressRequest request = new AddressRequest("경기도 성남시 분당구", "판교역로 231", true, "카카오");

        // when
        AddressResponse savedResponse = addressService.addAddress(request, testUser);

        // then
        assertThat(savedResponse.getAdId()).isNotNull();
        System.out.println("=== 실제 DB 저장 성공! 생성된 배송지 ID: " + savedResponse.getAdId() + " ===");

        Address foundAddress = addressRepository.findById(savedResponse.getAdId()).orElseThrow();
        assertThat(foundAddress.getAddress()).isEqualTo("경기도 성남시 분당구");
    }

    @Test
    @DisplayName("실제 DB에서 배송지 소프트 딜리트 테스트")
    void deleteAddress_RealDB_Test() {
        // given
        AddressRequest request = new AddressRequest("서울시 강남구", "테헤란로 123", false, "우리집");
        AddressResponse saved = addressService.addAddress(request, testUser);
        UUID adId = saved.getAdId();

        // when
        addressService.deleteAddress(adId);

        // then
        Address deletedAddress = addressRepository.findById(adId).orElseThrow();
        assertThat(deletedAddress.isDeleted()).isTrue();
        assertThat(deletedAddress.getDeletedAt()).isNotNull();
        System.out.println("=== 실제 DB 소프트 딜리트 성공! 삭제된 배송지 ID: " + adId + " ===");
    }
}
