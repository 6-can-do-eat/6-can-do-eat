package com.team6.backend.address.presentation.controlller;

import com.team6.backend.address.application.service.AddressService;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.auth.presentation.dto.UserDetailsImpl;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AddressController {

    private final AddressService addressService;

    /* 배송지 생성 */
    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<AddressResponse>> createAddress(@RequestBody AddressRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        AddressResponse response = addressService.addAddress(request, userDetails.getUser());
        URI alterUrl = URI.create("api/v1/users/" + response.getAdId());
        SuccessResponse<AddressResponse> successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "배송지 생성이 완료되었습니다.", response);
        return ResponseEntity.created(alterUrl).body(successResponse);
    }

    /* 배송지 삭제 (소프트) */
    @DeleteMapping("/addresses/{adId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID adId) {
        addressService.deleteAddress(adId);
        return ResponseEntity.noContent().build();
    }

    /* 배송지 수정 */
    @PutMapping("/addresses/{adId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<AddressResponse>> updateAddress(@PathVariable UUID adId, @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(adId, request);
        SuccessResponse<AddressResponse> successResponse = SuccessResponse.of(CommonSuccessCode.OK, "주소 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 내 배송지 목록 조회 */
    @GetMapping("/users/{userId}/addresses")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    public ResponseEntity<SuccessResponse<Page<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID userId,
            @RequestParam(required = false) String alias,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortByTime,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        // 본인 확인 (경로의 userId와 로그인한 유저 ID 비교)
        if (!userDetails.getUser().getId().equals(userId)) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        Page<AddressResponse> addresses = addressService.getAddress(userDetails.getUser(), alias, page, size, sortByTime, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(addresses));
    }

    /* 배송지 상세 조회 */
    @GetMapping("/addresses/{adId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<AddressResponse>> getAddressById(@PathVariable UUID adId) {
        AddressResponse response = addressService.getAddressById(adId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 기본 배송지 설정 */
    @PatchMapping("/addresses/{adId}/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<AddressResponse>> updateDefaultAddress(@PathVariable UUID adId) {
        AddressResponse response = addressService.UpdateDefault(adId);
        SuccessResponse<AddressResponse> successResponse = SuccessResponse.of(CommonSuccessCode.OK, "기본 배송지 설정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }
}
