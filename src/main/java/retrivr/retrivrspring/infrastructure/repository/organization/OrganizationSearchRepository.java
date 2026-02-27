package retrivr.retrivrspring.infrastructure.repository.organization;

import retrivr.retrivrspring.application.vo.OrganizationSearchResultWithRank;

import java.util.List;

public interface OrganizationSearchRepository {

  List<OrganizationSearchResultWithRank> searchRankedFirstPageByKeyword(String keyword, int limit);

  List<OrganizationSearchResultWithRank> searchRankedNextPageByKeyword(String keyword, int bucket,
      double sim, long organizationId, int limit);

  List<String> findSuggestions(String keyword, int limit);
}