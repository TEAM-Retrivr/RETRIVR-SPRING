package retrivr.retrivrspring.presentation.rental.req;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.springframework.lang.Nullable;

public record PublicRentalCreateRequest(
    Long itemUnitId,

    @Schema(
        description = "대여자 정보 입력 필드(key-value 형태)",
        example = """
            {
              "name": "홍길동",
              "phone": "010-1234-5678",
              "studentNumber": "202012345"
            }
            """
    )
    Map<String, String> renterFields
) {

}