package retrivr.retrivrspring.presentation.organization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.organization.OrganizationSearchService;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.organization.res.OrganizationSearchPageResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/v1/organizations/search")
@Tag(name = "Public API / Organization", description = "")
public class PublicOrganizationSearchController {

  private final OrganizationSearchService organizationSearchService;

  @GetMapping
  @Operation(summary = "대여지 검색")
  @ApiResponse(
      responseCode = "200",
      description = "검색 결과",
      content = @Content(schema = @Schema(implementation = OrganizationSearchPageResponse.class)))
  @ApiErrorCodeExamples({ErrorCode.NO_SEARCH_KEYWORD_EXCEPTION, ErrorCode.BLANK_SEARCH_KEYWORD_EXCEPTION, ErrorCode.DO_NOT_ENCODED_SEARCH_CURSOR, ErrorCode.INVALID_SEARCH_CURSOR})
  public OrganizationSearchPageResponse searchRankedPageByKeyword(
      @Parameter(description = "검색 키워드" , example = "건국대학교 도서관자치위원회")
      @RequestParam(name = "keyword") String keyword,
      @Parameter(description = "인코딩된 복합 커서. 다음 페이지 조회 시 사용. null 일 경우 첫번째 페이지 조회", example = "eyJidWNrZXQiOjMsInNpbSI6MC4zMTgxODIsIm9yZ2FuaXphdGlvbklkIjo1fQ")
      @RequestParam(name = "cursor", required = false)
      String cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size
  ) {
    return organizationSearchService.searchRankedPageByKeyword(keyword, cursor, size);
  }

  @GetMapping("/suggestions")
  @Operation(summary = "대여지 검색어 자동완성")
  @ApiResponse(
      responseCode = "200",
      description = "자동완성 결과",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))
  )
  public List<String> getSearchSuggestions(
      @RequestParam(name = "keyword") String keyword
  ) {
    return List.of(
        "건국대학교 도서관",
        "건국대학교 공학 도서관",
        "건국대학교 도서관자치위원회"
    );
  }
}
