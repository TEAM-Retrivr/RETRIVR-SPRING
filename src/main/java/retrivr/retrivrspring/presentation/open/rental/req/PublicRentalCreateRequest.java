package retrivr.retrivrspring.presentation.open.rental.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "공개 대여 생성 요청")
public record PublicRentalCreateRequest(
    @Schema(
        description = "대여할 개별 물품 유닛 ID. UNIT 관리 방식인 경우에만 전달합니다.",
        example = "12",
        nullable = true
    )
    Long itemUnitId,

    @Schema(
        description = "대여자 이름",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
    String name,

    @Schema(
        description = "대여자 전화번호. 숫자, 하이픈(-), 공백, + 입력이 가능합니다.",
        example = "010-1234-5678",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(
        regexp = "^[0-9\\-+ ]{7,20}$",
        message = "전화번호 형식이 올바르지 않습니다."
    )
    String phone,

    @Schema(
        description = "물품별 추가 대여자 정보. key는 필드명, value는 입력값입니다.",
        example = """
            {
              "학과": "컴퓨터공학부",
              "학번": "202012345"
            }
            """
    )
    Map<
        @NotBlank(message = "rentalFields의 key 는 공백일 수 없습니다.")
        String,

        @NotBlank(message = "rentalFields의 value 는 공백일 수 없습니다.")
        String> renterFields
) {

}