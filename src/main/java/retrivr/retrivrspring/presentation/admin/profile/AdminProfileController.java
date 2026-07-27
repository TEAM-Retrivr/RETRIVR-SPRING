package retrivr.retrivrspring.presentation.admin.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrivr.retrivrspring.application.service.admin.profile.AdminProfileService;
import retrivr.retrivrspring.application.service.admin.profile.PasswordVerificationService;
import retrivr.retrivrspring.presentation.admin.auth.AdminRefreshTokenCookieManager;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileImageUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminGetPresignedURLForUploadRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminCodeUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordVerificationRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileImageUpdateResponse;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminGetPresignedURLForUploadResponse;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminPasswordVerificationResponse;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/v1/profile")
@Tag(name = "Admin API / Profile", description = "관리자 프로필 API")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;
    private final PasswordVerificationService passwordVerificationService;
    private final AdminRefreshTokenCookieManager refreshTokenCookieManager;

    @GetMapping
    @Operation(
            summary = "관리자 프로필 조회",
            description = "개인정보 수정 화면에 표시할 단체 명을 조회합니다."
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
            summary = "관리자 단체명 수정",
            description = "로그인한 관리자의 단체명을 개별 수정합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "수정 성공"
    )
    @ApiErrorCodeExamples({
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.INVALID_VALUE_EXCEPTION
    })
    public ResponseEntity<Void> updateProfile(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminProfileUpdateRequest request
    ) {
        adminProfileService.updateProfile(authUser.organizationId(), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    @Operation(
            summary = "관리자 비밀번호 변경",
            description = "PASSWORD_CHANGE 목적으로 발급받은 비밀번호 인증 토큰을 검증하고 비밀번호를 변경합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "비밀번호 변경 및 세션 만료 성공"
    )
    @ApiErrorCodeExamples({
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH,
            ErrorCode.PASSWORD_RESET_POLICY_VIOLATION,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_INVALID,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_EXPIRED,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED
    })
    public ResponseEntity<Void> updatePassword(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminPasswordUpdateRequest request
    ) {
        adminProfileService.updatePassword(authUser.organizationId(), request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieManager.delete().toString())
                .build();
    }

    @PatchMapping("/admin-code")
    @Operation(
            summary = "관리자 코드 변경",
            description = "ADMIN_CODE_CHANGE 목적으로 발급받은 비밀번호 인증 토큰을 검증하고 관리자 코드를 변경합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "관리자 코드 변경 성공"
    )
    @ApiErrorCodeExamples({
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.ADMIN_CODE_MISMATCH,
            ErrorCode.INVALID_VALUE_EXCEPTION,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_INVALID,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_EXPIRED,
            ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED
    })
    public ResponseEntity<Void> updateAdminCode(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminCodeUpdateRequest request
    ) {
        adminProfileService.updateAdminCode(authUser.organizationId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/verify")
    @Operation(
            summary = "개인정보 변경용 현재 비밀번호 확인",
            description = "현재 비밀번호를 확인하고 변경 목적에 한정된 5분 유효 일회용 인증 토큰을 발급합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "비밀번호 확인 성공",
            content = @Content(schema = @Schema(implementation = AdminPasswordVerificationResponse.class))
    )
    @ApiErrorCodeExamples({
            ErrorCode.NOT_FOUND_ORGANIZATION,
            ErrorCode.PASSWORD_MISMATCH,
            ErrorCode.INVALID_VALUE_EXCEPTION
    })
    public AdminPasswordVerificationResponse verifyPassword(
            @Parameter(hidden = true) @AuthOrg AuthUser authUser,
            @Valid @RequestBody AdminPasswordVerificationRequest request
    ) {
        return passwordVerificationService.verify(authUser.organizationId(), request);
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
