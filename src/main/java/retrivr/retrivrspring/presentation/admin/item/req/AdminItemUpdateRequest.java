package retrivr.retrivrspring.presentation.admin.item.req;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "관리자 물품 수정 요청")
public record AdminItemUpdateRequest(

        String name,
        String description,
        Integer rentalDuration,
        Boolean isActive,

        @Schema(description = "대여자 요구 정보(JSONB)")
        List<AdminItemCreateRequest.BorrowerRequirement> borrowerRequirements
) {}
