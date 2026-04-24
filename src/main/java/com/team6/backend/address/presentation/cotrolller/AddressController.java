package com.team6.backend.address.presentation.cotrolller;

import com.team6.backend.address.application.service.AddressService;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.auth.presentation.dto.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

    // TODO: @Vaild 추가해야 하는 곳 확인하기....
    private final AddressService addressService;

    /*배송지 생성*/
    @PostMapping("/addresses")
    @PreAuthorize("hasRole(CUSTOMER)")
    public ResponseEntity<SuccessResponse<AddressResponse>> createAddress(@RequestBody AddressRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        AddressResponse response = addressService.addAddress(request, userDetails.getUser());
        // URL 작성해서 넘길지 (즉, 목록으로 바로 가는지 고민하기) -> 가야지
        URI alterUrl = URI.create("api/v1/users" + response.getAdId());
        SuccessResponse Suresponse = SuccessResponse.of(CommonSuccessCode.CREATED, "배송지 생성이 완료되었습니다.", response);
        return ResponseEntity.created(alterUrl).body(Suresponse);
    }

    // 삭제(소프트)
    @DeleteMapping("/addresses/{adId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    public ResponseEntity<SuccessResponse<Void>> deleteAddress(@PathVariable UUID adId){
        addressService.deleteAddress(adId);
        return ResponseEntity.noContent().build();
    }

    // 수정
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/addresses/{adId}")
    public ResponseEntity<SuccessResponse<AddressResponse>> updateAddress(@PathVariable UUID adId, @RequestBody AddressRequest request){
        AddressResponse response = addressService.updateAddress(adId,request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "주소 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    // 목록 조회
    // TODO: 로그인 한 유저랑 현 주소에 잇는 유저랑 같은 지 확인 로직 필요(서비스)
    // 검색이 필수 조건
    @GetMapping("/users/{userId}/addresses")
    public ResponseEntity<SuccessResponse<Page<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID userId,
            @RequestParam(required = false) String alias,
            @RequestParam(required = false,defaultValue = "0") int page,
            @RequestParam(required = false,defaultValue = "10") int size,
            @RequestParam(required = false,defaultValue = "createdAt") String sortByTime,
            @RequestParam(required = false,defaultValue = "false") boolean isAsc
    ){
        Page<AddressResponse> addresses = addressService.getAddress(userDetails.getUser(),alias,page,size,sortByTime,isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(addresses));
    }

    // 상세 조회
    @GetMapping("/addresses/{adId}")
    public ResponseEntity<SuccessResponse<AddressResponse>> getAddressById(@PathVariable UUID adId){
        AddressResponse response = addressService.getAddressById(adId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    // 기본 배송지 설정 -> boolean
    // 아이디, request
    @PreAuthorize("hasRole(CUSTOMER)")
    @PatchMapping("/addresses/{adId}/default")
    public ResponseEntity<SuccessResponse<AddressResponse>> updateDefaultAddress(@PathVariable UUID adId, @RequestBody AddressRequest request){
        AddressResponse response = addressService.UpdateDefault(adId);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, " 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }
}