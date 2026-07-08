package retrivr.retrivrspring.presentation.admin.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.account.AdminWithdrawalService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.account.req.AdminWithdrawRequest;
import retrivr.retrivrspring.presentation.admin.account.res.AdminWithdrawResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/account")
@Tag(name = "Admin API / Account", description = "관리자 계정 관리 API")
public class AdminAccountController {

    private final AdminWithdrawalService adminWithdrawalService;

    @PostMapping("/withdraw")
    @Operation(
            summary = "관리자 계정 탈퇴",
            description = "비밀번호를 최종 확인한 뒤 탈퇴 사유를 저장하고 계정을 탈퇴 상태로 변경한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "탈퇴 성공",
            content = @Content(schema = @Schema(implementation = AdminWithdrawResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.ACCOUNT_NOT_FOUND,
            ErrorCode.ACCOUNT_WITHDRAWN,
            ErrorCode.PASSWORD_MISMATCH,
            ErrorCode.WITHDRAW_REASON_REQUIRED,
            ErrorCode.WITHDRAW_OTHER_REASON_REQUIRED
    })
    public AdminWithdrawResponse withdraw(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminWithdrawRequest request
    ) {
        return adminWithdrawalService.withdraw(authUser, request);
    }
}
