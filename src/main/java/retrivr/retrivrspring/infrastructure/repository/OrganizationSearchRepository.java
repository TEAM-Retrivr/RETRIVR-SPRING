package retrivr.retrivrspring.infrastructure.repository;

import java.util.List;
import retrivr.retrivrspring.application.vo.OrganizationSearchResultWithRank;

public interface OrganizationSearchRepository {

  List<OrganizationSearchResultWithRank> searchRankedFirstPageByKeyword(String keyword, int limit);

  List<OrganizationSearchResultWithRank> searchRankedNextPageByKeyword(String keyword, int bucket,
      double sim, long organizationId, int limit);
}