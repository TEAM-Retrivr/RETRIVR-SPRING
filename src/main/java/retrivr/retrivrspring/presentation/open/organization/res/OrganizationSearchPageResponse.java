package retrivr.retrivrspring.presentation.open.organization.res;

import java.util.List;
import retrivr.retrivrspring.domain.entity.organization.Organization;

public record OrganizationSearchPageResponse(
    List<OrganizationSearchSummary> organizations,
    String nextCursor
) {

  public record OrganizationSearchSummary(
      Long organizationId,
      String name,
      String imageURL
  ) {

    public static OrganizationSearchSummary from(Organization organization) {
      return new OrganizationSearchSummary(
          organization.getId(),
          organization.getName(),
          organization.getProfileImageKey()
      );
    }
  }
}
