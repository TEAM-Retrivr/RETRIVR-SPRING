package retrivr.retrivrspring.presentation.internal.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record InternalCouponCreateRequest(

    @Schema(
        description = "쿠폰 이름",
        example = "30일 무료 이용권"
    )
    @NotBlank
    String name,

    @Schema(
        description = "쿠폰 사용 가이드라인",
        example = "신규 가입 조직만 사용 가능합니다."
    )
    @NotBlank
    String guideline,

    @Schema(
        description = "쿠폰 설명",
        example = "Retrivr Pro 30일 무료 체험 쿠폰"
    )
    @NotBlank
    String description,

    @Schema(
        description = "총 사용 가능 수량",
        example = "100"
    )
    @Min(1)
    int totalQuantity,

    @Schema(
        description = "이용 기간(일)",
        example = "30"
    )
    @Min(1)
    int durationDays,

    @Schema(
        description = "쿠폰 활성 시작일",
        example = "2026-05-01"
    )
    @NotNull
    LocalDate activeStartAt,

    @Schema(
        description = "쿠폰 만료일",
        example = "2026-12-31"
    )
    @NotNull
    LocalDate expiresAt
) {

}
