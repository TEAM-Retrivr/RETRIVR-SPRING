package retrivr.retrivrspring.presentation.organization.res;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrganizationSearchResponse(
    @Schema(example = "1")
    Long id,
    @Schema(example = "건국대학교 도서관자치위원회")
    String name,
    @Schema(example = "https://s3.northeast/image", description = "이미지 url")
    String imageUrl
) {

}
