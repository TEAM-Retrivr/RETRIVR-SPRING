package retrivr.retrivrspring.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationResponse;

import java.security.SecureRandom;
import java.time.LocalDateTime;

//email 인증을 redis 기반으로 바꿀 수 있을지도
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final OrganizationRepository organizationRepository;

    public void sendCode(String email) {

        Organization organization = organizationRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.EMAIL_NOT_FOUND)
                );

        LocalDateTime now = LocalDateTime.now();

        emailVerificationRepository
                .findTopByOrganizationOrderByCreatedAtDesc(organization)
                .ifPresent(latest -> {
                    if (latest.getCreatedAt().isAfter(now.minusSeconds(60))) {
                        throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
                    }
                });

        String code = generateCode();

        EmailVerification verification = EmailVerification.create(
                organization,
                code,
                now.plusMinutes(10)
        );

        emailVerificationRepository.save(verification);
    }

    public EmailVerificationResponse verify(EmailVerificationRequest request) {

        Organization organization = organizationRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.EMAIL_NOT_FOUND)
                );

        EmailVerification verification = emailVerificationRepository
                .findTopByOrganizationOrderByCreatedAtDesc(organization)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND)
                );

        LocalDateTime now = LocalDateTime.now();

        if (verification.isExpired(now)) {
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (verification.isVerified()) {
            throw new ApplicationException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (!verification.getCode().equals(request.code())) {
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        verification.markVerified(now);

        return new EmailVerificationResponse(
                request.email(),
                true,
                verification.getVerifiedAt()
        );
    }
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

}