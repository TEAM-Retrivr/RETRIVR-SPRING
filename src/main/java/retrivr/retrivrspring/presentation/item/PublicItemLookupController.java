package retrivr.retrivrspring.presentation.item;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.presentation.item.res.PublicItemListPageResponse;
import retrivr.retrivrspring.presentation.item.res.PublicItemSummaryResponse;

@RestController
@Tag(name = "Public API / Item", description = "대여자용 물품 조회/대여 요청")
@RequestMapping("/api/public/v1")
public class PublicItemLookupController {

  @GetMapping("/organizations/{organizationId}/items")
  @Operation(summary = "대여지(단체) 물품 목록 조회")
  @ApiResponse(
      responseCode = "200",
      description = "물품 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = PublicItemListPageResponse.class))
  )
  public PublicItemListPageResponse getOrgItems(
      @PathVariable Long organizationId,
      @Parameter(description = "커서(마지막 조회된 itemId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size
  ) {
    return new PublicItemListPageResponse(
        List.of(
            // itemUnit 없는 "수량형" 아이템 예시
            new PublicItemSummaryResponse(
                11L,                 // itemId
                10,                  // totalQuantity
                3,                   // availableQuantity
                "우산",               // name
                3,                   // duration (예: 3일)
                "비 오는 날 대여 가능한 우산", // description
                "학생증",             // guaranteedGoods (보증물)
                true,                // isRentable
                List.of()            // itemUnits (없으면 빈 리스트)
            ),

            // itemUnit 있는 "개별 코드형" 아이템 예시
            new PublicItemSummaryResponse(
                12L,
                2,
                1,
                "보조배터리",
                1,
                "20000mAh 보조배터리",
                "신분증",
                true,                // 전체적으로는 대여 가능
                List.of(
                    new PublicItemSummaryResponse.PublicItemUnitResponse(
                        1201L,
                        "BAT-001",
                        true
                    ),
                    new PublicItemSummaryResponse.PublicItemUnitResponse(
                        1202L,
                        "BAT-002",
                        false            // 이 유닛은 현재 대여 불가(대여중/고장/분실 등)
                    )
                )
            ),

            // 재고 0이라 대여 불가 예시
            new PublicItemSummaryResponse(
                13L,
                5,
                0,
                "실습실 키",
                1,
                "실습실 출입용 키",
                "학생증",
                false,               // availableQuantity=0이면 false로 맞춰두는 게 자연스러움
                List.of()
            )
        ),
        13L // nextCursor (마지막 itemId)
    );
  }
}
