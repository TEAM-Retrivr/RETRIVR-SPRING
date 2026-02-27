package retrivr.retrivrspring.presentation.organization.res;

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
      //todo: Organization 도메인에 img 추가 (figma 에 따라서 필요)
      return new OrganizationSearchSummary(
          organization.getId(),
          organization.getName(),
          null
      );
    }
  }
}
