package retrivr.retrivrspring.presentation.message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.message.SendMessageService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/developer/v1/messages")
@Tag(name = "Developer API / Message", description = "메시지 발송 관리")
public class MessageSendController {

  private final SendMessageService sendMessageService;

  @PostMapping("/overdue-reminder")
  @Operation(summary = "전체 연체 알림 메시지 수동 발송")
  @ApiResponse(
      responseCode = "200",
      description = "연체 알림 메시지 발송 성공"
  )
  @ApiErrorCodeExamples({
      ErrorCode.NOT_FOUND_ORGANIZATION
  })
  public ResponseEntity<Void> sendOverdueReminders(
      @Parameter(hidden = true) @AuthOrg AuthUser authUser
  ) {
    sendMessageService.sendAllOverdueReminders();
    return ResponseEntity.ok().build();
  }
}
