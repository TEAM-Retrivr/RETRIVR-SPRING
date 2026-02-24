package retrivr.retrivrspring.application.vo;

import retrivr.retrivrspring.domain.entity.organization.Organization;

public record OrganizationSearchResultWithRank(
    Organization organization,
    int bucket,
    double sim
) {

}
