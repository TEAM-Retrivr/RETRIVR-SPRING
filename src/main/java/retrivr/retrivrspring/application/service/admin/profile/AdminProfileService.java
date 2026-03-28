package retrivr.retrivrspring.application.service.admin.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.AdminAuthCodeHash;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordHash;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminProfileUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminProfileResponse;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminProfileService {
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

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
}
