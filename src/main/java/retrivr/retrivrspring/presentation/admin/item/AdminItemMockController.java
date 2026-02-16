package retrivr.retrivrspring.presentation.admin.item;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.admin.item.request.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.request.AdminItemUpdateRequest;
import retrivr.retrivrspring.presentation.admin.item.response.AdminItemCreateResponse;
import retrivr.retrivrspring.presentation.admin.item.response.AdminItemListResponse;
import retrivr.retrivrspring.presentation.admin.item.response.AdminItemUpdateResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/items")
@Tag(name = "Admin API / Item", description = "관리자 물품 관리 API")
public class AdminItemMockController {

    @GetMapping
    @Operation(
            summary = "UC-2.1 관리자 물품 리스트 조회",
            description = "관리자가 등록한 물품 목록을 조회한다. 각 물품의 총 수량과 대여 가능 수량을 포함한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "물품 리스트 조회 성공",
            content = @Content(
                    schema = @Schema(implementation = AdminItemListResponse.class)
            )
    )
    public List<AdminItemListResponse> getItems() {

        return List.of(
                new AdminItemListResponse(
                        11L,
                        "노트북",
                        10,
                        7,
                        true
                ),
                new AdminItemListResponse(
                        12L,
                        "보조배터리",
                        5,
                        0,
                        true
                ),
                new AdminItemListResponse(
                        13L,
                        "실습실 키",
                        3,
                        3,
                        false
                )
        );
    }

    @PostMapping
    @Operation(
            summary = "UC-2.2 관리자 물품 등록",
            description = "물품과 대여자 요구 정보(JSONB)를 함께 저장한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "물품 등록 성공",
            content = @Content(schema = @Schema(implementation = AdminItemCreateResponse.class))
    )
    public AdminItemCreateResponse createItem(
            @Valid @RequestBody AdminItemCreateRequest request
    ) {

        Long mockItemId = 101L;

        return new AdminItemCreateResponse(
                mockItemId,
                request.name(),
                request.borrowerRequirements()
        );
    }


    @PatchMapping("/{itemId}")
    @Operation(
            summary = "UC-2.3 관리자 물품 수정",
            description = "기존 물품 정보를 수정한다. 부분 수정이 가능하다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "물품 수정 성공",
            content = @Content(schema = @Schema(implementation = AdminItemUpdateResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "잘못된 입력값"
    )
    public AdminItemUpdateResponse updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody AdminItemUpdateRequest request
    ) {

        // Mock 검증 예시
        if (request.totalQuantity() != null && request.totalQuantity() <= 0) {
            throw new IllegalArgumentException("총 수량은 1 이상이어야 합니다.");
        }

        return new AdminItemUpdateResponse(
                itemId,
                request.name() != null ? request.name() : "기존 물품명",
                request.totalQuantity() != null ? request.totalQuantity() : 10,
                request.isActive() != null ? request.isActive() : true
        );
    }


}
