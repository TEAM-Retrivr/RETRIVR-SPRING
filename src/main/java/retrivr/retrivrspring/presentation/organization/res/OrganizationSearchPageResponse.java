package retrivr.retrivrspring.presentation.organization.res;

import java.util.List;

public record OrganizationSearchPageResponse(
    List<OrganizationSearchSummary> organizations,
    Long nextCursor
) {
  public record OrganizationSearchSummary(
      Long organizationId,
      String name,
      String imageURL
  ) {

  }
}
