package retrivr.retrivrspring.presentation.admin.item.response;

import io.swagger.v3.oas.annotations.media.Schema;
import retrivr.retrivrspring.presentation.admin.item.request.AdminItemCreateRequest;

import java.util.List;

@Schema(description = "관리자 물품 수정 응답")
public record AdminItemUpdateResponse(

        Long itemId,
        String name,
        List<AdminItemCreateRequest.BorrowerRequirement> borrowerRequirements
) {}

