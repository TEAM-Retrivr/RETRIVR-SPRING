package retrivr.retrivrspring.application.service.admin.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import retrivr.retrivrspring.application.port.image.ImageStoragePort;
import retrivr.retrivrspring.application.port.image.PresignedUploadUrl;
import retrivr.retrivrspring.application.port.image.ProfileImageKeyGeneratorPort;
import retrivr.retrivrspring.domain.entity.organization.AdminAuthCodeHash;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordHash;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.infrastructure.image.ImageContentTypePolicy;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileImageUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminGetPresignedURLForUploadRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileImageUpdateResponse;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminGetPresignedURLForUploadResponse;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileResponse;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageKeyGeneratorPort profileImageKeyGeneratorPort;
    private final ImageStoragePort imageStoragePort;

    @Transactional(readOnly = true)
    public AdminProfileResponse getProfile(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        return new AdminProfileResponse(
                organization.getName(),
                organization.getProfileImageKey(),
                organization.getEmail()
        );
    }

    @Transactional
    public AdminProfileResponse updateProfile(Long organizationId, AdminProfileUpdateRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        String normalizedEmail = request.newEmail().trim().toLowerCase(Locale.ROOT);
        String organizationName = request.newOrganizationName().trim();
        PasswordHash newPasswordHash = PasswordHash.fromRawOrThrow(
                request.newPassword(),
                passwordEncoder,
                ErrorCode.INVALID_VALUE_EXCEPTION
        );
        AdminAuthCodeHash newAdminAuthCodeHash = AdminAuthCodeHash.fromRawOrThrow(
                request.newAdminCode(),
                passwordEncoder
        );

        if (organizationName.isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApplicationException(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH);
        }

        organizationRepository.findByEmail(normalizedEmail)
                .filter(found -> !found.getId().equals(organizationId))
                .ifPresent(found -> {
                    throw new ApplicationException(ErrorCode.ALREADY_EXIST_EXCEPTION);
                });

        organization.updateProfile(
                normalizedEmail,
                newPasswordHash.getValue(),
                organizationName,
                newAdminAuthCodeHash.getValue()
        );

        return new AdminProfileResponse(
                organization.getName(),
                organization.getProfileImageKey(),
                organization.getEmail()
        );
    }

    @Transactional
    public AdminGetPresignedURLForUploadResponse getPresignedURLForUpload(Long loginOrganizationId, AdminGetPresignedURLForUploadRequest request) {
        // 로그인한 조직에 대한 검증
        Organization organization = organizationRepository.findById(loginOrganizationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        // Image Content Type 에 대한 검증
        if (!ImageContentTypePolicy.isAllowed(request.imageContentType())) {
            throw new ApplicationException(ErrorCode.NOT_ALLOWED_IMAGE_CONTENT_TYPE);
        }

        // 이미지 파일 확장자 추출
        String imageFileExtension = ImageContentTypePolicy.extractExtension(request.imageContentType());
        
        // S3 에 저장할 경로(objectKey) 생성
        String objectKey = profileImageKeyGeneratorPort.generate(loginOrganizationId, imageFileExtension);

        // 이미지 업로드용 Presigned URL 발급
        PresignedUploadUrl presignedUploadUrl = imageStoragePort.createPresignedUploadUrl(objectKey, request.imageContentType());

        return new AdminGetPresignedURLForUploadResponse(loginOrganizationId, presignedUploadUrl.uploadUrl());
    }

    @Transactional
    public AdminProfileImageUpdateResponse updateProfileImage(Long loginOrganizationId, AdminProfileImageUpdateRequest request) {
        // 로그인한 조직에 대한 검증
        Organization organization = organizationRepository.findById(loginOrganizationId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

        // objectKey 가 null 일 경우 이미지 삭제
        if (request.objectKey() == null) {
            imageStoragePort.delete(organization.getProfileImageKey());
            organization.updateProfileImageKey(null);
            return new AdminProfileImageUpdateResponse(loginOrganizationId, null);
        }

        // 이미지의 ObjectKey 가 로그인한 조직의 것인지에 대한 검증
        if (!profileImageKeyGeneratorPort.isProfileImageKeyOwner(loginOrganizationId, request.objectKey())) {
            throw new ApplicationException(ErrorCode.ORGANIZATION_MISMATCH_EXCEPTION);
        }

        // ObjectKey 가 S3에 존재하는지 검증
        if (!imageStoragePort.exists(request.objectKey())) {
            throw new ApplicationException(ErrorCode.NOT_FOUND_PROFILE_IMAGE);
        }

        // 이전 이미지 삭제
        deleteAfterCommit(organization.getProfileImageKey());

        // 이미지 업데이트
        organization.updateProfileImageKey(request.objectKey());

        // 다운로드용 Presigned URL 발급
        String downloadUrl = imageStoragePort.createPresignedDownloadUrl(request.objectKey());

        return new AdminProfileImageUpdateResponse(
            loginOrganizationId,
            downloadUrl
        );
    }

    private void deleteAfterCommit(String objectKey) {
        if (objectKey == null) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //todo: 이후 outbox 도입하여 고아 이미지 삭제 배치 구현
                try {
                    imageStoragePort.delete(objectKey);
                } catch (Exception e) {
                    log.warn("이전 프로필 이미지 삭제 실패. objectKey={}", objectKey, e);
                }
            }
        });
    }
}
