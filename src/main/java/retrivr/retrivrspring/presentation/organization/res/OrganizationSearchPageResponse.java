package retrivr.retrivrspring.presentation.organization.res;

import java.util.List;

public record OrganizationSearchPageResponse(
    List<OrganizationSearchResponse> organizations,
    Long nextCursor
) {

}
