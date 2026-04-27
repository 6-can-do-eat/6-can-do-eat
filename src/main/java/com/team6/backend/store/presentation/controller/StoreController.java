package com.team6.backend.store.presentation.controller;

import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.presentation.dto.request.StoreRequest;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Store", description = "가게 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    /* 가게 생성 */
    @Operation(
            summary = "가게 신규 생성",
            description = "가맹점주가 새로운 가게를 시스템에 등록합니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `OWNER`\n" +
                    "- 소유자 할당: 현재 로그인한 사용자가 해당 가게의 주인으로 자동 등록됩니다.\n" +
                    "- 초기 상태: 평점은 0.0으로 설정되며, 숨김 상태(isHidden)는 `false`로 시작합니다.\n\n" +
                    "**[발생 가능한 비즈니스 에러]**\n" +
                    "- `USER_NOT_FOUND`: 로그인된 사용자 정보를 찾을 수 없을 때\n" +
                    "- `STORE_NOT_FOUND`: 입력한 `categoryId` 또는 `areaId`가 존재하지 않을 때",
            responses = {
                    @ApiResponse(responseCode = "201", description = "가게 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "필수 필드 누락 또는 형식 오류",
                            content = @Content(examples = {
                                    @ExampleObject(name = "이름 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"가게 이름은 필수입니다.\"}"),
                                    @ExampleObject(name = "카테고리 ID 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"카테고리 ID는 필수입니다.\"}"),
                                    @ExampleObject(name = "지역 ID 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"지역 ID는 필수입니다.\"}"),
                                    @ExampleObject(name = "주소 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"가게 주소는 필수입니다.\"}")
                            })),
                    @ApiResponse(responseCode = "403", description = "권한 부족 (OWNER 아님)",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"COMMON_403\", \"status\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "참조 데이터 없음",
                            content = @Content(examples = {
                                    @ExampleObject(name = "사용자 없음", value = "{\"code\": \"USER_404\", \"status\": \"NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}"),
                                    @ExampleObject(name = "카테고리/지역 없음", value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")
                            }))
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('OWNER')") // Role이 OWNER인 사용자만 호출 가능
    public ResponseEntity<SuccessResponse<StoreResponse>> createStore(@RequestBody @Valid StoreRequest request) {
        StoreResponse response = storeService.createStore(request);
        URI uri = URI.create("/api/v1/stores/" + response.getStoreId());
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "가게 생성이 완료되었습니다.", response);
        return ResponseEntity.created(uri).body(successResponse);
    }

    /* 가게 목록 조회 */
    @Operation(
            summary = "가게 목록 전체 조회",
            description = "모든 가게를 조회합니다.\n\n" +
                    "**[조회 특징]**\n" +
                    "- 숨김 및 삭제 상태인 가게는 결과에서 제외됩니다.\n" +
                    "- 검색 필터: 이름 키워드, 카테고리 ID, 지역 ID를 통한 필터링을 지원합니다.",
            parameters = {
                    @Parameter(name = "keyword", description = "가게 이름 검색어 (대소문자 무시)"),
                    @Parameter(name = "categoryId", description = "카테고리 ID 필터링"),
                    @Parameter(name = "areaId", description = "지역 ID 필터링"),
                    @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
                    @Parameter(name = "size", description = "페이지 당 데이터 개수", example = "10"),
                    @Parameter(name = "sortBy", description = "정렬 기준 (기본: createdAt)", example = "average_rating"),
                    @Parameter(name = "isAsc", description = "오름차순 여부", example = "false")
            }
    )
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<StoreResponse>>> getStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<StoreResponse> stores = storeService.getStores(keyword, categoryId, areaId, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(stores));
    }

    /* 가게 목록 조회 (본인 소유 가게만) */
    @Operation(
            summary = "본인 소유 가게 목록 조회 (점주용)",
            description = "현재 로그인한 가맹점주가 소유한 가게 목록을 조회합니다.\n\n" +
                    "**[조회 특징]**\n" +
                    "- 권한: `OWNER`(본인)\n" +
                    "- 숨김 처리된 가게를 포함하여 본인의 모든 가게를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족 (OWNER 아님)",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"COMMON_403\", \"status\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다.\"}")))
            }
    )
    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SuccessResponse<Page<StoreResponse>>> getStoresForOwner(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<StoreResponse> stores = storeService.getStoresForOwner(keyword, categoryId, areaId, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(stores));
    }

    /* 가게 상세 조회 */
    @Operation(
            summary = "가게 상세 조회",
            description = "가게 고유 ID를 통해 특정 가게의 상세 정보를 조회합니다.\n\n" +
                    "**[비즈니스 에러]**\n" +
                    "- `STORE_NOT_FOUND`: 존재하지 않거나 삭제된 가게 ID 조회 시",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "가게 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")))
            }
    )
    @GetMapping("/{storeId}")
    public ResponseEntity<SuccessResponse<StoreResponse>> getStoreById(@PathVariable UUID storeId) {
        StoreResponse response = storeService.getStoreById(storeId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 가게 정보 수정 */
    @Operation(
            summary = "가게 정보 수정",
            description = "가게의 기본 정보(이름, 주소, 카테고리, 지역)를 수정합니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `OWNER`(본인), `MANAGER`, `MASTER`\n\n" +
                    "**[비즈니스 에러]**\n" +
                    "- `STORE_FORBIDDEN`: 해당 가게에 대한 수정 권한이 없음",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "입력 데이터 오류",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"가게 이름은 필수입니다.\"}"))),
                    @ApiResponse(responseCode = "403", description = "수정 권한 부족 (소유자 아님)",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_403\", \"status\": \"FORBIDDEN\", \"message\": \"가게에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "가게/카테고리/지역 미존재",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PutMapping("/{storeId}")
    public ResponseEntity<SuccessResponse<StoreResponse>> updateStore(@PathVariable UUID storeId, @RequestBody @Valid StoreRequest request) {
        StoreResponse response = storeService.updateStore(storeId, request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "가게 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 가게 삭제 (소프트) */
    @Operation(
            summary = "가게 삭제 (소프트 삭제)",
            description = "가게를 시스템에서 삭제 처리합니다.\n\n" +
                    "**[권한 및 처리 방식]**\n" +
                    "- 권한: `OWNER`(본인), `MASTER`\n" +
                    "- 방식: `deleted_at`을 기록하고 조회에서 제외하는 소프트 삭제 방식입니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "삭제 권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_403\", \"status\": \"FORBIDDEN\", \"message\": \"가게에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "가게 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<SuccessResponse<Void>> deleteStore(@PathVariable UUID storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build(); // 삭제 작업은 예외적으로 SuccessResponse 없이 상태 코드 204를 반환하는 것으로 결정되었습니다.
    }

    /* 가게 숨김 처리 */
    @Operation(
            summary = "가게 숨김 상태 변경",
            description = "가게를 검색 결과에 노출할지 여부를 토글합니다.\n\n" +
                    "**[권한]**\n" +
                    "- `OWNER`(본인), `MANAGER`, `MASTER`\n" +
                    "- 호출 시 마다 `isHidden` 값이 반전됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "변경 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_403\", \"status\": \"FORBIDDEN\", \"message\": \"가게에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "가게 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/{storeId}/hide")
    public ResponseEntity<SuccessResponse<StoreResponse>> hideStore(@PathVariable UUID storeId) {
        StoreResponse response = storeService.hideStore(storeId);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "가게 숨김 변경이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

}
