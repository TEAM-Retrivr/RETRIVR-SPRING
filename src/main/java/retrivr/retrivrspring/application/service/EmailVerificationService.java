package retrivr.retrivrspring.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.EmailVerification;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;
import retrivr.retrivrspring.domain.entity.organization.SignupToken;
import retrivr.retrivrspring.domain.repository.EmailVerificationRepository;
import retrivr.retrivrspring.domain.repository.SignupTokenRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.EmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailCodeVerifyResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationResponse;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

//todo email 인증을 redis 기반으로 바꿀 수 있을지도
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private static final int EXPIRES_SECONDS = 600;
    private static final long RESEND_BLOCK_SECONDS = 60;

    private final EmailVerificationRepository emailVerificationRepository;
    private final SignupTokenRepository signupTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public EmailVerificationSendResponse sendCode(EmailVerificationSendRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        EmailVerificationPurpose purpose = request.purpose();

        LocalDateTime now = LocalDateTime.now();

        //        String rawCode = generateCode();
        String rawCode = "123456"; //todo : 개발용 고정 코드 이메일 연결 하십쇼
        String hashedCode = passwordEncoder.encode(rawCode);

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
                .map(existing -> {
                    // 재전송 제한
                    if (existing.getUpdatedAt() != null && existing.getUpdatedAt().isAfter(now.minusSeconds(RESEND_BLOCK_SECONDS))) {
                        throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
                    }

                    existing.refresh(hashedCode, now.plusSeconds(EXPIRES_SECONDS));
                    return existing;
                })
                .orElseGet(() ->
                        EmailVerification.create(
                                email,
                                purpose,
                                hashedCode,
                                now.plusSeconds(EXPIRES_SECONDS)
                        )
                );

        emailVerificationRepository.save(verification);
        return new EmailVerificationSendResponse(email, purpose.name(), EXPIRES_SECONDS);

        // TODO: rawCode를 이메일로 발송
    }


    public Object verify(EmailVerificationRequest request) {

        String email = request.email();
        EmailVerificationPurpose purpose = request.purpose();
        String code = request.code();

        EmailVerification verification = emailVerificationRepository
                .findByEmailAndPurpose(email, purpose)
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

        if (!passwordEncoder.matches(code, verification.getCode())) {
            throw new ApplicationException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        verification.markVerified(now);

        // SIGNUP이면 signupToken 생성
        if (purpose == EmailVerificationPurpose.SIGNUP) {

            String rawSignupToken = "st_" + UUID.randomUUID();
            String signupTokenHash = passwordEncoder.encode(rawSignupToken);

            signupTokenRepository.deleteByEmail(email);

            SignupToken token = SignupToken.builder()
                    .email(email)
                    .tokenHash(signupTokenHash)
                    .expiresAt(now.plusMinutes(10))
                    .build();

            token.markCodeVerified(now);

            signupTokenRepository.save(token);

            return new EmailCodeVerifyResponse(rawSignupToken, EXPIRES_SECONDS);
        }

        // 그 외 purpose는 일반 인증 응답
        return new EmailVerificationResponse(
                email,
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