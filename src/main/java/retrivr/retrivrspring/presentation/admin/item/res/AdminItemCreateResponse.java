package retrivr.retrivrspring.presentation.admin.item.res;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemCreateRequest;

import java.util.List;

@Schema(description = "관리자 물품 등록 응답")
public record AdminItemCreateResponse(

        @Schema(example = "101")
        Long itemId,

        @Schema(example = "C타입 충전기")
        String name,

        @Schema(description = "대여자 요구 정보(JSONB)")
        List<AdminItemCreateRequest.BorrowerRequirement> borrowerRequirements
) {}

