package com.team6.backend.area.presentation.controller;

import com.team6.backend.area.application.service.AreaService;
import com.team6.backend.area.presentation.dto.request.AreaCreateRequest;
import com.team6.backend.area.presentation.dto.request.UpdateAreaRequest;
import com.team6.backend.area.presentation.dto.response.AreaResponse;
import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    // 지역 등록
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    @PostMapping
    public ResponseEntity<SuccessResponse<AreaResponse>> createArea(
            @Valid @RequestBody AreaCreateRequest request
    ) {
        AreaResponse response = areaService.createArea(request);
        return ResponseEntity.ok(SuccessResponse.of(CommonSuccessCode.CREATED, response));
    }

    // 지역 목록 조회
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<AreaResponse>>> getAreas(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        Page<AreaResponse> response = areaService.getAreas(keyword, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.of(CommonSuccessCode.OK, response));
    }

    // 지역 상세 조회
    @GetMapping("/{areaId}")
    public ResponseEntity<SuccessResponse<AreaResponse>> getArea(
            @PathVariable UUID areaId
    ) {
        AreaResponse response = areaService.getAreaById(areaId);
        return ResponseEntity.ok(SuccessResponse.of(CommonSuccessCode.OK, response));
    }

    // 지역 수정 - MASTER, MANAGER
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    @PutMapping("/{areaId}")
    public ResponseEntity<SuccessResponse<AreaResponse>> updateArea(
            @PathVariable UUID areaId,
            @Valid @RequestBody UpdateAreaRequest request
    ) {
        AreaResponse response = areaService.updateArea(areaId, request);
        return ResponseEntity.ok(SuccessResponse.of(CommonSuccessCode.OK, response));
    }

    // 지역 삭제 (소프트) - MASTER
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{areaId}")
    public ResponseEntity<SuccessResponse<Void>> deleteArea(
            @PathVariable UUID areaId
    ) {
        areaService.deleteArea(areaId);
        return ResponseEntity.ok(SuccessResponse.of(CommonSuccessCode.OK, null));
    }
}