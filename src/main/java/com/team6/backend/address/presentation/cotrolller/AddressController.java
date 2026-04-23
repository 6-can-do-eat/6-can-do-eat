package com.team6.backend.address.presentation.cotrolller;

import com.team6.backend.address.application.service.AddressService;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.user.presentation.dto.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    /*배송지 생성*/
    //TODO: 기본 배송지 설정은 따로 메서드 만들어서 해결하기
    @PostMapping
    @PreAuthorize("hasRole(CUSTOMER)")
    public ResponseEntity<SuccessResponse<AddressResponse>> createAddress(@RequestBody AddressRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        AddressResponse response = addressService.addAddress(request, userDetails.getUser());
        // TODO: URL 작성해서 넘길지 (즉, 목록으로 바로 가는지 고민하기)
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    // 삭제(소프트)
    @DeleteMapping("/{adId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    public ResponseEntity<SuccessResponse<Void>> deleteAddress(@PathVariable UUID adId){
        addressService.deleteAddress(adId);
        return ResponseEntity.noContent().build();
    }

    // 수정
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("{adId}")
    public ResponseEntity<SuccessResponse<AddressResponse>> updateAddress(@PathVariable UUID adId, @RequestBody AddressRequest request){
        AddressResponse response = addressService.updateAddress(adId,request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "주소 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    // 목록 조회

    // 상세 조회

    // 기본 배송지 설정 -> boolean
    // 아이디, request
    @PreAuthorize("hasRole(CUSTOMER)")
    @PatchMapping("{adId}/default")
    public ResponseEntity<SuccessResponse<AddressResponse>> updateDefaultAddress(@PathVariable UUID adId, @RequestBody AddressRequest request){
        AddressResponse response = addressService.UpdateDefault(adId);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, " 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }
}
