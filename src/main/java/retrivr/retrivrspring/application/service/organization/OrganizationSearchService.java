package retrivr.retrivrspring.application.service.organization;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.NormalizedOrganizationSearchRequest;
import retrivr.retrivrspring.application.vo.OrganizationSearchResultWithRank;
import retrivr.retrivrspring.infrastructure.repository.OrganizationRepository;
import retrivr.retrivrspring.application.vo.OrganizationSearchCursor;
import retrivr.retrivrspring.presentation.organization.res.OrganizationSearchPageResponse;
import retrivr.retrivrspring.presentation.organization.res.OrganizationSearchPageResponse.OrganizationSearchSummary;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizationSearchService {

  private final OrganizationRepository organizationRepository;

  public OrganizationSearchPageResponse searchRankedPageByKeyword(String keyword, String cursor, Integer size) {

    log.debug("[OrganizationSearch] start keyword='{}', cursor={}, size={}",
        keyword, cursor, size);

    NormalizedOrganizationSearchRequest nrq = NormalizedOrganizationSearchRequest.of(keyword, size);
    OrganizationSearchCursor decodedCursor = OrganizationSearchCursor.decode(cursor);

    log.debug("[OrganizationSearch] normalized keyword='{}', size={}, sizePlusOne={}",
        nrq.keyword(), nrq.size(), nrq.sizePlusOne());

    List<OrganizationSearchResultWithRank> fetchedOrg;
    if (decodedCursor == null) {
      log.debug("[OrganizationSearch] first page search");
      fetchedOrg = organizationRepository.searchRankedFirstPageByKeyword(nrq.keyword(), nrq.sizePlusOne());
    } else {
      log.debug("[OrganizationSearch] cursor search bucket={}, sim={}, orgId={}",
          decodedCursor.bucket(),
          decodedCursor.sim(),
          decodedCursor.organizationId());

      fetchedOrg = organizationRepository.searchRankedNextPageByKeyword(
          nrq.keyword(),
          decodedCursor.bucket(),
          decodedCursor.sim(),
          decodedCursor.organizationId(),
          nrq.sizePlusOne());
    }

    boolean hasNext = fetchedOrg.size() > nrq.size();
    List<OrganizationSearchResultWithRank> content =
        hasNext ? fetchedOrg.subList(0, size) : fetchedOrg;

    List<OrganizationSearchSummary> organizations = content.stream()
        .map(row -> OrganizationSearchSummary.from(row.organization()))
        .toList();

    String nextCursor = null;
    if (hasNext && !content.isEmpty()) {
      OrganizationSearchResultWithRank last = content.getLast();
      nextCursor = new OrganizationSearchCursor(last.bucket(), last.sim(),
          last.organization().getId()
      ).encode();

      log.debug("[OrganizationSearch] nextCursor generated");
    }

    return new OrganizationSearchPageResponse(organizations, nextCursor);
  }

  public List<String> getSuggestions(String keyword, int size) {
    if (keyword == null || keyword.trim().length() < 2) {
      log.debug("[OrganizationSearchSuggestions] keyword is too short");
      return List.of();
    }
    return organizationRepository.findSuggestions(keyword, size);
  }
}
