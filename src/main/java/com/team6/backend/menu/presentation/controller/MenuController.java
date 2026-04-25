package com.team6.backend.menu.presentation.controller;

import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.menu.application.service.MenuService;
import com.team6.backend.menu.presentation.dto.request.MenuRequest;
import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import com.team6.backend.menu.presentation.dto.response.MenuResponse;
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

@Tag(name = "Menu", description = "메뉴 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MenuController {

    private final MenuService menuService;

    /* 메뉴 등록 (AI 설명 생성 옵션) */
    @Operation(
            summary = "메뉴 등록 (AI 설명 생성 지원)",
            description = "특정 가게에 새로운 메뉴를 추가합니다.\n\n" +
                    "**[AI 연동 로직]**\n" +
                    "- `aiDescription`이 `true`인 경우: `aiPrompt`를 반드시 입력해야 하며, 시스템이 AI를 통해 설명을 자동 생성합니다.\n" +
                    "- `aiDescription`이 `false`인 경우: 입력된 `description`을 그대로 사용합니다.\n\n" +
                    "**[권한 및 제약]**\n" +
                    "- 권한: `OWNER`(본인)만 등록 가능합니다.\n" +
                    "- 유효성: 본인 소유의 가게(`storeId`)인지 검증합니다.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "메뉴 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                            content = @Content(examples = {
                                    @ExampleObject(name = "이름 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"메뉴 이름은 필수입니다.\"}"),
                                    @ExampleObject(name = "가격 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"가격은 필수입니다.\"}"),
                                    @ExampleObject(name = "AI 프롬프트 누락", value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"프롬프트를 입력해 주세요.\"}")
                            })),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_403\", \"status\": \"FORBIDDEN\", \"message\": \"메뉴에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "가게 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/stores/{storeId}/menus")
    public ResponseEntity<SuccessResponse<MenuResponse>> createMenu(@PathVariable UUID storeId, @RequestBody @Valid MenuRequest request) {
        MenuResponse response = menuService.createMenu(storeId, request);
        URI uri = URI.create("/api/v1/menus/" + response.getMenuId());
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "메뉴 생성이 완료되었습니다.", response);
        return ResponseEntity.created(uri).body(successResponse);
    }

    /* 메뉴 목록 조회 */
    @Operation(
            summary = "가게별 메뉴 목록 조회",
            description = "가게별 메뉴 목록을 페이징 조회합니다.\n\n" +
                    "**[특징]**\n" +
                    "- 숨김 처리된 메뉴는 제외하고 반환합니다.\n" +
                    "- 키워드 검색을 통해 메뉴명을 필터링할 수 있습니다.",
            parameters = {
                    @Parameter(name = "storeId", description = "조회할 가게의 ID"),
                    @Parameter(name = "keyword", description = "메뉴명 검색어"),
                    @Parameter(name = "page", description = "페이지 번호 (0부터)"),
                    @Parameter(name = "size", description = "페이지 당 개수 (기본 10)"),
                    @Parameter(name = "sortBy", description = "정렬 필드 (기본 createdAt)"),
                    @Parameter(name = "isAsc", description = "오름차순 여부")
            }
    )
    @GetMapping("/stores/{storeId}/menus")
    public ResponseEntity<SuccessResponse<Page<MenuResponse>>> getMenus(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<MenuResponse> menus = menuService.getMenus(storeId, keyword, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(menus));
    }

    /* 메뉴 목록 조회 (본인 가게 메뉴 조회) */
    @Operation(
            summary = "점주용 메뉴 목록 조회",
            description = "가맹점주가 본인 가게의 전체 메뉴 리스트를 조회합니다.\n\n" +
                    "**[특징]**\n" +
                    "- 권한: `OWNER`(본인)만 호출 가능합니다.\n" +
                    "- 숨김 처리된 메뉴를 포함하여 전체 메뉴를 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_403\", \"status\": \"FORBIDDEN\", \"message\": \"가게에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "가게 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 가게가 존재하지 않습니다.\"}")))
            }
    )
    @GetMapping("/stores/{storeId}/menus/my")
    public ResponseEntity<SuccessResponse<Page<MenuResponse>>> getMenusForOwner(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<MenuResponse> menus = menuService.getMenusForOwner(storeId, keyword, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(menus));
    }

    /* 메뉴 상세 조회 */
    @Operation(
            summary = "메뉴 상세 조회",
            description = "메뉴 고유 ID를 통해 특정 메뉴의 상세 정보를 조회합니다.\n\n" +
                    "**[비즈니스 에러]**\n" +
                    "- `MENU_404`: 존재하지 않거나 삭제된 메뉴 ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "메뉴 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 메뉴가 존재하지 않습니다.\"}")))
            }
    )
    @GetMapping("/menus/{menuId}")
    public ResponseEntity<SuccessResponse<MenuResponse>> getMenuById(@PathVariable UUID menuId) {
        MenuResponse response = menuService.getMenuById(menuId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 메뉴 수정 */
    @Operation(
            summary = "메뉴 정보 수정",
            description = "기존 메뉴의 이름, 가격, 설명을 수정합니다.\n\n" +
                    "**[권한 정책]**\n" +
                    "- 수정 가능 등급: `OWNER`(본인), `MANAGER`, `MASTER`\n",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "입력 데이터 오류",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"메뉴 이름은 필수입니다.\"}"))),
                    @ApiResponse(responseCode = "403", description = "수정 권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_403\", \"status\": \"FORBIDDEN\", \"message\": \"메뉴에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "메뉴 미존재",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 메뉴가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PutMapping("/menus/{menuId}")
    public ResponseEntity<SuccessResponse<MenuResponse>> updateMenu(@PathVariable UUID menuId, @RequestBody @Valid UpdateMenuRequest request) {
        MenuResponse response = menuService.updateMenu(menuId, request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "메뉴 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 메뉴 삭제 (소프트) */
    @Operation(
            summary = "메뉴 삭제 (소프트 삭제)",
            description = "메뉴를 삭제 처리합니다.\n\n" +
                    "**[권한 정책]**\n" +
                    "- 삭제 가능 등급: `OWNER`(본인), `MASTER`\n",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "삭제 권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_403\", \"status\": \"FORBIDDEN\", \"message\": \"메뉴에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "메뉴 미존재",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 메뉴가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<SuccessResponse<Void>> deleteMenu(@PathVariable UUID menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.noContent().build();
    }

    /* 메뉴 숨김 처리 */
    @Operation(
            summary = "메뉴 숨김/노출 상태 변경",
            description = "일반 사용자에게 메뉴를 노출할지 여부를 결정합니다.\n\n" +
                    "**[권한 정책]**\n" +
                    "- 권한: `OWNER`(본인), `MANAGER`, `MASTER`\n" +
                    "- 호출 시마다 `isHidden` 값이 반전됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "변경 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_403\", \"status\": \"FORBIDDEN\", \"message\": \"메뉴에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "메뉴 미존재",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"MENU_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 메뉴가 존재하지 않습니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/menus/{menuId}/hide")
    public ResponseEntity<SuccessResponse<MenuResponse>> hideMenu(@PathVariable UUID menuId) {
        MenuResponse response = menuService.hideMenu(menuId);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "메뉴 숨김 변경이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

}
