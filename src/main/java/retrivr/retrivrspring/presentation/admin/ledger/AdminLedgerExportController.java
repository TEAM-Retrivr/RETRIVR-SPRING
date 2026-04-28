package retrivr.retrivrspring.presentation.admin.ledger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.ledger.AdminLedgerExportService;
import retrivr.retrivrspring.application.vo.LedgerByteArrayAndFileName;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/ledgers")
@Tag(name = "Admin API / Ledger", description = "장부 엑셀 다운로드 API")
public class AdminLedgerExportController {

  private final AdminLedgerExportService adminLedgerExportService;

  @GetMapping("/excel")
  @Operation(
      summary = "장부 엑셀 다운로드",
      description = """
          대여 이력, 미반납 현황, 물품 현황, 요약 통계 시트를 포함한
          장부 엑셀 파일(.xlsx)을 다운로드한다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "엑셀 다운로드 성공",
      content = @Content(
          mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      )
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION
  })
  public ResponseEntity<ByteArrayResource> exportLedgerExcel(
      @Parameter(hidden = true) @AuthOrg AuthUser loginUser
  ) {
    LedgerByteArrayAndFileName data = adminLedgerExportService.exportLedger(loginUser.organizationId());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(data.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentLength(data.resource().contentLength())
        .body(data.resource());
  }
}
