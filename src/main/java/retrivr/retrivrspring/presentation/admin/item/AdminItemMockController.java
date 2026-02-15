package retrivr.retrivrspring.presentation.admin.item;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import retrivr.retrivrspring.presentation.admin.item.request.AdminItemCreateRequest;
import retrivr.retrivrspring.presentation.admin.item.response.*;

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
            description = "관리자가 새로운 물품을 등록한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "물품 등록 성공",
            content = @Content(schema = @Schema(implementation = AdminItemCreateResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "입력값 오류"
    )
    public AdminItemCreateResponse createItem(
            @Valid @RequestBody AdminItemCreateRequest request
    ) {

        if (request.totalQuantity() <= 0) {
            throw new IllegalArgumentException("총 수량은 1 이상이어야 합니다.");
        }

        Long mockItemId = 101L;

        return new AdminItemCreateResponse(
                mockItemId,
                request.name(),
                request.totalQuantity()
        );
    }



}
