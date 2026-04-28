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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.profile.AdminProfileService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileImageUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminGetPresignedURLForUploadRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileImageUpdateResponse;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminGetPresignedURLForUploadResponse;
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

    @PostMapping("/images/pre-signed-upload-url")
    @Operation(
        summary = "관리자 프로필 사진 업로드용 Presigned URL 발급"
    )
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = AdminGetPresignedURLForUploadResponse.class))
    )
    @ApiErrorCodeExamples({
        ErrorCode.NOT_FOUND_ORGANIZATION,
        ErrorCode.NOT_ALLOWED_IMAGE_CONTENT_TYPE
    })
    public AdminGetPresignedURLForUploadResponse getPresignedURLForUpload(
        @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
        @Valid @RequestBody AdminGetPresignedURLForUploadRequest request
    ) {
        return adminProfileService.getPresignedURLForUpload(loginUser.organizationId(), request);
    }

    @PutMapping("/images")
    @Operation(
        summary = "관리자 프로필 이미지 수정 확정",
        description = "관리자 프로필을 입력된 ObjectKey 로 대체합니다."
    )
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = AdminProfileImageUpdateResponse.class))
    )
    @ApiErrorCodeExamples({
        ErrorCode.NOT_FOUND_ORGANIZATION,
        ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION,
        ErrorCode.NOT_FOUND_PROFILE_IMAGE
    })
    public AdminProfileImageUpdateResponse updateProfileImage(
        @Parameter(hidden = true) @AuthOrg AuthUser loginUser,
        @Valid @RequestBody AdminProfileImageUpdateRequest request
    ) {
        return adminProfileService.updateProfileImage(loginUser.organizationId(), request);
    }
}
