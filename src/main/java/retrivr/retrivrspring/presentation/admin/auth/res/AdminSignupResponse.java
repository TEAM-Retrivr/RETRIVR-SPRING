package retrivr.retrivrspring.presentation.admin.auth.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminSignupResponse(

        @Schema(example = "2", description = "organization.org_id")
        Long orgId,

        @Schema(example = "건국대학교 도서관자치위원회")
        String organizationName,

        @Schema(example = "admin@retrivr.com")
        String email,

        @Schema(example = "PENDING")
        String status
) {}
