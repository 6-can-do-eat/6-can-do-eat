package com.team6.backend.address.presentation.cotrolller;

import com.team6.backend.address.application.service.addressService;
import com.team6.backend.address.presentation.dto.addressRequest;
import com.team6.backend.address.presentation.dto.addressResponse;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.user.presentation.dto.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/addresses")
public class addressController {

    private final addressService addressService;

    /*배송지 생성*/
    //TODO: 기본 배송지 설정은 따로 메서드 만들어서 해결하기
    @PostMapping
    @PreAuthorize("hasRole(CUSTOMER)")
    public ResponseEntity<SuccessResponse<addressResponse>> createAddress(@RequestBody addressRequest request,@AuthenticationPrincipal UserDetailsImpl userDetails) {
        addressResponse response = addressService.addAddress(request, userDetails.getUser());
        // TODO: URL 작성해서 넘길지 (즉, 목록으로 바로 가는지 고민하기)
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }
}
