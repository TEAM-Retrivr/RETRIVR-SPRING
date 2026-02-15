package retrivr.retrivrspring.presentation.organization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.presentation.organization.res.OrganizationSearchPageResponse;
import retrivr.retrivrspring.presentation.organization.res.OrganizationSearchPageResponse.OrganizationSearchSummary;

@RestController
@RequestMapping("/api/public/v1/organizations/search")
@Tag(name = "Public API / Organization", description = "")
public class PublicOrganizationSearchController {

  @GetMapping
  @Operation(summary = "대여지 검색")
  @ApiResponse(
      responseCode = "200",
      description = "검색 결과",
      content = @Content(schema = @Schema(implementation = OrganizationSearchPageResponse.class)))
  public OrganizationSearchPageResponse search(
      @RequestParam(name = "keyword") String keyword,
      @Parameter(description = "커서(마지막 조회된 orgId). 다음 페이지 조회 시 사용", example = "2")
      @RequestParam(name = "cursor", required = false)
      Long cursor,
      @RequestParam(name = "size", required = false, defaultValue = "15") Integer size
  ) {
    return new OrganizationSearchPageResponse(
        List.of(
          new OrganizationSearchSummary(1L, "건국대학교 도서관자치위원회", "https://s3.northeast/image/dojawi"),
          new OrganizationSearchSummary(2L, "건국대학교 동아리연합회", "https://s3.northeast/image/clubunion")
        ),
        2L
    );
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
