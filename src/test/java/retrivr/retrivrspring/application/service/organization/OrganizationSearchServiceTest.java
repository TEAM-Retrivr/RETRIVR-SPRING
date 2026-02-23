package retrivr.retrivrspring.application.service.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.application.vo.OrganizationSearchCursor;
import retrivr.retrivrspring.application.vo.OrganizationSearchResultWithRank;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.infrastructure.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.presentation.organization.res.OrganizationSearchPageResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationSearchServiceTest {

  @Mock
  private OrganizationRepository organizationRepository;

  @InjectMocks
  private OrganizationSearchService organizationSearchService;

  private Organization mockOrg(long id, String name) {
    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn(id);
    when(org.getName()).thenReturn(name);
    return org;
  }

  private OrganizationSearchResultWithRank mockRow(int bucket, double sim, long orgId, String name) {
    Organization org = mockOrg(orgId, name);
    OrganizationSearchResultWithRank row = mock(OrganizationSearchResultWithRank.class);
    when(row.bucket()).thenReturn(bucket);
    when(row.sim()).thenReturn(sim);
    when(row.organization()).thenReturn(org);
    return row;
  }

  @Test
  @DisplayName("TC-01: 첫 페이지 조회(cursor=null) + hasNext=true 이면 nextCursor 생성")
  void search_firstPage_hasNext_true() {
    // given
    String keyword = "abc";
    Integer size = 2;
    String cursor = null;
    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(size);

    List<OrganizationSearchResultWithRank> fetched = List.of(
        mockRow(1, 0.91, 101L, "동연"),
        mockRow(1, 0.90, 100L, "도자위"),
        mockRow(1, 0.89,  99L, " 총학") // size+1
    );

    when(organizationRepository.searchRankedFirstPageByKeyword(eq(keyword), eq(normalizedSize.sizePlusOne())))
        .thenReturn(fetched);

    // when
    OrganizationSearchPageResponse res =
        organizationSearchService.searchRankedPageByKeyword(keyword, cursor, size);

    // then
    assertThat(res.organizations()).hasSize(size);
    assertThat(res.nextCursor()).isNotNull();

    verify(organizationRepository, times(1))
        .searchRankedFirstPageByKeyword(eq(keyword), eq(normalizedSize.sizePlusOne()));
    verify(organizationRepository, never())
        .searchRankedNextPageByKeyword(anyString(), anyInt(), anyDouble(), anyLong(), anyInt());
  }

  @Test
  @DisplayName("TC-02: 첫 페이지 조회(cursor=null) + hasNext=false 이면 nextCursor=null")
  void search_firstPage_hasNext_false() {
    // given
    String keyword = "abc";
    Integer size = 3;

    List<OrganizationSearchResultWithRank> fetched = List.of(
        mockRow(1, 0.91, 101L, "동연"),
        mockRow(1, 0.90, 100L, "도자위"),
        mockRow(1, 0.89,  99L, " 총학")
        // size+1이 아니라 size만 → hasNext=false
    );

    when(organizationRepository.searchRankedFirstPageByKeyword(eq(keyword), eq(size + 1)))
        .thenReturn(fetched);

    // when
    OrganizationSearchPageResponse res =
        organizationSearchService.searchRankedPageByKeyword(keyword, null, size);

    // then
    assertThat(res.organizations()).hasSize(size);
    assertThat(res.nextCursor()).isNull();

    verify(organizationRepository).searchRankedFirstPageByKeyword(eq(keyword), eq(size + 1));
  }

  @Test
  @DisplayName("TC-03: 다음 페이지 조회(cursor!=null) + hasNext=true 이면 nextCursor 생성")
  void search_nextPage_hasNext_true() {
    // given
    String keyword = "abc";
    Integer size = 2;

    // 커서에 들어갈 값(서비스는 decode 후 repo에 그대로 전달해야 함)
    int bucket = 7;
    double sim = 0.777;
    long orgId = 555L;
    String cursor = new OrganizationSearchCursor(bucket, sim, orgId).encode();

    List<OrganizationSearchResultWithRank> fetched = List.of(
        mockRow(bucket, 0.70, 200L, "동연"),
        mockRow(bucket, 0.69, 199L, "도자위"),
        mockRow(bucket, 0.68, 198L, "총학") // size+1
    );

    when(organizationRepository.searchRankedNextPageByKeyword(
        eq(keyword),
        eq(bucket),
        eq(sim),
        eq(orgId),
        eq(size + 1)
    )).thenReturn(fetched);

    // when
    OrganizationSearchPageResponse res =
        organizationSearchService.searchRankedPageByKeyword(keyword, cursor, size);

    // then
    assertThat(res.organizations()).hasSize(2);
    assertThat(res.nextCursor()).isNotNull();

    verify(organizationRepository, times(1))
        .searchRankedNextPageByKeyword(eq(keyword), eq(bucket), eq(sim), eq(orgId), eq(size + 1));
    verify(organizationRepository, never())
        .searchRankedFirstPageByKeyword(anyString(), anyInt());
  }

  @Test
  @DisplayName("TC-04: 다음 페이지 조회(cursor!=null) + 결과 비어있으면 nextCursor=null")
  void search_nextPage_empty() {
    // given
    String keyword = "abc";
    Integer size = 2;

    int bucket = 1;
    double sim = 0.5;
    long orgId = 10L;
    String cursor = new OrganizationSearchCursor(bucket, sim, orgId).encode();

    when(organizationRepository.searchRankedNextPageByKeyword(
        eq(keyword), eq(bucket), eq(sim), eq(orgId), eq(size + 1)
    )).thenReturn(List.of());

    // when
    OrganizationSearchPageResponse res =
        organizationSearchService.searchRankedPageByKeyword(keyword, cursor, size);

    // then
    assertThat(res.organizations()).isEmpty();
    assertThat(res.nextCursor()).isNull();
  }

  @Test
  @DisplayName("TC-05: getSuggestions - keyword=null 이면 빈 리스트 + repo 호출 없음")
  void suggestions_keyword_null() {
    // when
    List<String> res = organizationSearchService.getSuggestions(null, 10);

    // then
    assertThat(res).isEmpty();
    verify(organizationRepository, never()).findSuggestions(anyString(), anyInt());
  }

  @Test
  @DisplayName("TC-06: getSuggestions - trim 후 길이<2 이면 빈 리스트 + repo 호출 없음")
  void suggestions_keyword_too_short() {
    // when
    List<String> res = organizationSearchService.getSuggestions(" a ", 10);

    // then
    assertThat(res).isEmpty();
    verify(organizationRepository, never()).findSuggestions(anyString(), anyInt());
  }

  @Test
  @DisplayName("TC-07: getSuggestions - 정상 입력이면 repo 결과 반환")
  void suggestions_ok() {
    // given
    when(organizationRepository.findSuggestions("ab", 3))
        .thenReturn(List.of("abc", "abd"));

    // when
    List<String> res = organizationSearchService.getSuggestions("ab", 3);

    // then
    assertThat(res).containsExactly("abc", "abd");
    verify(organizationRepository, times(1)).findSuggestions("ab", 3);
  }

  @Test
  @DisplayName("보너스: 첫 페이지에서 sizePlusOne(size+1)가 repo로 전달되는지(인자 캡처)")
  void search_firstPage_argument_capture() {
    // 1) 먼저 row/list를 만든다 (여기서 내부 when() 호출이 끝남)
    OrganizationSearchResultWithRank row = mockRow(1, 0.9, 1L, "동연");
    List<OrganizationSearchResultWithRank> fetched = List.of(row);

    // 2) 그 다음 repo stubbing
    when(organizationRepository.searchRankedFirstPageByKeyword(anyString(), anyInt()))
        .thenReturn(fetched);

    ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> sizePlusOneCaptor = ArgumentCaptor.forClass(Integer.class);

    // when
    organizationSearchService.searchRankedPageByKeyword("abc", null, 5);

    // then
    verify(organizationRepository).searchRankedFirstPageByKeyword(keywordCaptor.capture(), sizePlusOneCaptor.capture());
    assertThat(keywordCaptor.getValue()).isEqualTo("abc");
    assertThat(sizePlusOneCaptor.getValue()).isEqualTo(6);
  }
}