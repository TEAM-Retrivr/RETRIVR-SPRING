package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "관리자 물품 등록 요청")
public record AdminItemCreateRequest(

        @Schema(description = "물품명", example = "C타입 충전기")
        @NotBlank
        String name,

        @Schema(description = "물품 설명", example = "고속 충전 지원")
        String description,

        @Schema(description = "대여 가능 기간 (일)", example = "3")
        @Min(1)
        int rentalDuration,

        @Schema(description = "활성 여부", example = "true")
        @NotNull
        Boolean isActive,

        @Schema(description = "대여자 요구 정보 설정(JSONB)")
        List<BorrowerRequirement> borrowerRequirements
) {

        @Schema(description = "대여자 입력 요구 항목")
        public record BorrowerRequirement(

                @Schema(example = "studentNumber")
                String fieldKey,

                @Schema(example = "학번")
                String label,

                @Schema(example = "TEXT")
                String fieldType,

                @Schema(example = "true")
                boolean required
        ) {}
}
