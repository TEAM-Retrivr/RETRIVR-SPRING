package retrivr.retrivrspring.presentation.admin.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.profile.AdminProfileService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/profile")
@Tag(name = "Admin API / Profile", description = "관리자 프로필 API")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    @Operation(
            summary = "관리자 프로필 조회",
            description = "단체 명, 단체 이미지, 단체 이메일을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AdminProfileResponse.class))
    )
    @ApiErrorCodeExamples({ErrorCode.NOT_FOUND_ORGANIZATION})
    public AdminProfileResponse getProfile(@Parameter(hidden = true) @AuthOrg AuthUser authUser) {
        return adminProfileService.getProfile(authUser.organizationId());
    }

    @PatchMapping
    @Operation(
            summary = "관리자 프로필 수정",
            description = "newEmail, newPassword, confirmPassword, newOrganizationName, newAdminCode를 수정합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "수정 성공",
            content = @Content(schema = @Schema(implementation = AdminProfileResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.INVALID_VALUE_EXCEPTION,
            ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH,
            ErrorCode.ALREADY_EXIST_EXCEPTION
    })
    public AdminProfileResponse updateProfile(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminProfileUpdateRequest request
    ) {
        return adminProfileService.updateProfile(authUser.organizationId(), request);
    }
}
