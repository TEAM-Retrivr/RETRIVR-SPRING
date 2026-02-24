package retrivr.retrivrspring.presentation.rental.req;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record PublicRentalCreateRequest(
    Long itemUnitId,
    String name,
    String phone,
    @Schema(
        description = "대여자 정보 입력 필드(key-value 형태)",
        example = """
            {
              "학과": "컴퓨터공학부",
              "학번": "202012345"
            }
            """
    )
    Map<String, String> renterFields
) {

}