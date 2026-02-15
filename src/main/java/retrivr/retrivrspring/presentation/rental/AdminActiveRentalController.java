package retrivr.retrivrspring.presentation.rental;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalItemPageResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalItemPageResponse.RentalItemSummary;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalReturnRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalReturnResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminOverdueRentalItemPageResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminOverdueRentalItemPageResponse.OverdueRentalItemSummary;
import retrivr.retrivrspring.presentation.rental.res.AdminReturnItemUnitListPageResponse.ReturnItemUnitSummary;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;
import retrivr.retrivrspring.presentation.rental.req.RentalItemUpdateDueDateRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalDueDateUpdateResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminReturnItemUnitListPageResponse;

@RestController
@RequestMapping("/api/admin/v1")
public class AdminActiveRentalController {

  @GetMapping("/rentals/overdue")
  @Operation(summary = "연체된 물품 조회")
  public AdminOverdueRentalItemPageResponse getOverdueItemList(
      @Parameter(description = "커서(마지막 조회된 rentalId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "10") Integer size
  ) {
    return new AdminOverdueRentalItemPageResponse(
        List.of(
            new OverdueRentalItemSummary(
                101L,
                10220L,
                "C타입 충전기",
                11L,
                "c타입 충전기(1)",
                "조윤아",
                "202312000",
                "동물자원과학과",
                LocalDate.now().minusDays(4),
                LocalDate.now().minusDays(1),
                1,
                new ArrayList<>(),
                true
            ),
            new OverdueRentalItemSummary(
                    101L,
                    10220L,
                    "C타입 충전기",
                    11L,
                    "c타입 충전기(2)",
                    "강찬욱",
                    "202112000",
                    "컴퓨터공학부",
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    5,
                    List.of(
                        LocalDate.now().minusDays(3),
                        LocalDate.now().minusDays(1)
                    ),
                    false
            )
        ), 101L
    );
  }

  @GetMapping("/items/rental-summary")
  public AdminRentalItemPageResponse getRentalItemSummaryList(
      @Parameter(description = "커서(마지막 조회된 itemId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "10") Integer size
  ) {
    return new AdminRentalItemPageResponse(
        List.of(
            new RentalItemSummary(
                99L,
                "C타입 충전기",
                5,
                2,
                true
            ),
            new RentalItemSummary(
                100L,
                "실험복",
                5,
                0,
                false
            )

        ),
        100L
    );
  }

  @GetMapping("/items/{itemId}/rentals/active")
  @Operation(summary = "대여 중인 물품 상세 조회")
  @ApiResponse(
      responseCode = "200",
      description = "반납 상세 조회 성공",
      content = @Content(schema = @Schema(implementation = AdminReturnItemUnitListPageResponse.class))
  )
  public AdminReturnItemUnitListPageResponse getReturnDetail(
      @PathVariable("itemId") Long itemId,
      @Parameter(description = "커서(마지막 조회된 itemUnitId). 다음 페이지 조회 시 사용", example = "10")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size
  ) {
    return new AdminReturnItemUnitListPageResponse(
        true,
        101L,
        "C타입 충전기",
        5,
        3,
        List.of(
            new ReturnItemUnitSummary(
                1L,
                "c타입 충전기(1)",
                "조윤아",
                "2023111111",
                "동물자원과학과",
                LocalDate.now().minusDays(4),
                LocalDate.now().minusDays(1)
            ),
            new ReturnItemUnitSummary(
                2L,
                "c타입 충전기(3)",
                "강찬욱",
                "2021111111",
                "컴퓨터공학부",
                LocalDate.now().minusDays(4),
                LocalDate.now().plusDays(2)
            )
        ),
        2L
    );
  }

  @PostMapping("/rentals/{rentalId}/return")
  @Operation(summary = "반납 확인")
  @ApiResponse(
      responseCode = "200",
      description = "반납 확인 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalReturnResponse.class))
  )
  public AdminRentalReturnResponse confirmReturn(
      @PathVariable("rentalId") Long rentalId,
      @RequestBody AdminRentalReturnRequest request
  ) {
    return new AdminRentalReturnResponse(
        rentalId,
        RentalStatus.RETURNED,
        request.adminNameToConfirm()
    );
  }

  @PatchMapping("/rentals/{rentalId}/due-date")
  @Operation(summary = "반납 일자 수정")
  @ApiResponse(
      responseCode = "200",
      description = "반납 예정일 수정 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalDueDateUpdateResponse.class))
  )
  public AdminRentalDueDateUpdateResponse updateDueDate(
      @PathVariable("rentalId") Long rentalId,
      @RequestBody RentalItemUpdateDueDateRequest request
  ) {
    return new AdminRentalDueDateUpdateResponse(
        rentalId,
        request.newReturnDueDate()
    );
  }
}
