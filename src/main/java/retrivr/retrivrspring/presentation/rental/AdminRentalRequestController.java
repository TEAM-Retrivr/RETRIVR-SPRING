package retrivr.retrivrspring.presentation.rental;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.entity.rental.enumerate.RentalDecisionStatus;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalApproveRequest;
import retrivr.retrivrspring.presentation.rental.req.AdminRentalRejectRequest;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalDecisionResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse;
import retrivr.retrivrspring.presentation.rental.res.AdminRentalRequestPageResponse.RentalRequestSummary;

@RestController
@RequestMapping("/api/admin/v1/rentals")
@Tag(name = "Admin API / Rental Request", description = "대여 요청 관리")
public class AdminRentalRequestController {

  @GetMapping("/requests")
  @Operation(summary = "대여 요청 목록 조회(요청됨 상태)")
  @ApiResponse(
      responseCode = "200",
      description = "대여 요청 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalRequestPageResponse.class))
  )
  public AdminRentalRequestPageResponse listRequested(
      @Parameter(description = "커서(마지막 조회된 rentalId). 다음 페이지 조회 시 사용", example = "1001")
      @RequestParam(name = "cursor", required = false) Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size
  ) {
    return new AdminRentalRequestPageResponse(
        List.of(
            new RentalRequestSummary(
                1001L,
                11L,
                "C타입 충전기",
                101L,
                "C타입 충전기(1)",
                5,
                3,
                "강찬욱",
                "컴퓨터공학부",
                "202111240",
                "학생증",
                LocalDateTime.now().minusHours(1)
            ),
            new RentalRequestSummary(
                1002L,
                12L,
                "실험복",
                104L,
                "실험복(1)",
                10,
                3,
                "박다솔",
                "컴퓨터공학부",
                "202311111",
                null,
                LocalDateTime.now().minusHours(1)
            )
        ),
        1002L
    );
  }

  @PostMapping("/{rentalId}/approve")
  @Operation(summary = "대여 요청 승인")
  @ApiResponse(
      responseCode = "200",
      description = "승인 처리 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalDecisionResponse.class))
  )
  public AdminRentalDecisionResponse approve(
      @PathVariable Long rentalId,
      @RequestBody AdminRentalApproveRequest request
  ) {
    return new AdminRentalDecisionResponse(
        rentalId,
        RentalDecisionStatus.APPROVE,
        request.adminNameToApprove(),
        LocalDateTime.now()
    );
  }

  @PostMapping("/{rentalId}/reject")
  @Operation(summary = "대여 요청 거부")
  @ApiResponse(
      responseCode = "200",
      description = "거부 처리 성공",
      content = @Content(schema = @Schema(implementation = AdminRentalDecisionResponse.class))
  )
  public AdminRentalDecisionResponse reject(
      @PathVariable Long rentalId,
      @RequestBody AdminRentalRejectRequest request
  ) {
    return new AdminRentalDecisionResponse(
        rentalId,
        RentalDecisionStatus.REJECT,
        request.adminNameToReject(),
        LocalDateTime.now()
    );
  }

}
