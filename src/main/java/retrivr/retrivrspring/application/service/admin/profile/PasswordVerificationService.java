package retrivr.retrivrspring.application.service.admin.profile;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.PasswordVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PasswordVerificationPurpose;
import retrivr.retrivrspring.domain.repository.auth.PasswordVerificationTokenRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordVerificationRequest;
import retrivr.retrivrspring.presentation.admin.profile.res.AdminPasswordVerificationResponse;

@Service
@RequiredArgsConstructor
public class PasswordVerificationService {

    private static final long TOKEN_EXPIRATION_SECONDS = 300;
    private static final String TOKEN_PREFIX = "pvt_";

    private final OrganizationRepository organizationRepository;
    private final PasswordVerificationTokenRepository passwordVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminPasswordVerificationResponse verify(
            Long organizationId,
            AdminPasswordVerificationRequest request
    ) {
        Organization organization = organizationRepository.findByIdForUpdate(organizationId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.NOT_FOUND_ORGANIZATION
                ));

        if (!passwordEncoder.matches(request.password(), organization.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.PASSWORD_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now();
        passwordVerificationTokenRepository.findByOrganizationAndPurpose(
                organization, request.purpose()
        ).ifPresent(existingToken -> {
            // 비밀번호를 다시 확인했다면 기존 토큰의 상태와 관계없이 교체한다.
            // 이전 화면이나 탭에 남아 있는 토큰은 새 토큰 발급 즉시 무효화된다.
            passwordVerificationTokenRepository.delete(existingToken);
            passwordVerificationTokenRepository.flush();
        });

        String rawToken = TOKEN_PREFIX + UUID.randomUUID();
        PasswordVerificationToken token = PasswordVerificationToken.builder()
                .organization(organization)
                .purpose(request.purpose())
                .tokenHash(passwordEncoder.encode(rawToken))
                .expiresAt(now.plusSeconds(TOKEN_EXPIRATION_SECONDS))
                .build();
        passwordVerificationTokenRepository.save(token);

        return new AdminPasswordVerificationResponse(rawToken, TOKEN_EXPIRATION_SECONDS);
    }

    /*
     * 이메일 인증 서비스의 noRollbackFor 정책에 참여할 때도 토큰 검증 실패가
     * 바깥 트랜잭션을 rollback-only 상태로 만들지 않도록 동일한 예외 정책을 적용한다.
     * 비밀번호·관리자 코드 변경 서비스에서는 예외가 바깥 @Transactional 경계를 통과하므로
     * 해당 변경 작업 전체가 기존과 동일하게 롤백된다.
     */
    @Transactional(readOnly = true, noRollbackFor = ApplicationException.class)
    public void validate(
            Long organizationId,
            PasswordVerificationPurpose purpose,
            String rawToken
    ) {
        findValidToken(organizationId, purpose, rawToken);
    }

    @Transactional(noRollbackFor = ApplicationException.class)
    public void validateAndConsume(
            Long organizationId,
            PasswordVerificationPurpose purpose,
            String rawToken
    ) {
        PasswordVerificationToken token = findValidToken(
                organizationId,
                purpose,
                rawToken,
                LocalDateTime.now()
        );
        int deletedRows = passwordVerificationTokenRepository.deleteByIdIfExists(token.getId());
        if (deletedRows == 0) {
            throw new ApplicationException(
                    ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED
            );
        }
    }

    private PasswordVerificationToken findValidToken(
            Long organizationId,
            PasswordVerificationPurpose purpose,
            String rawToken
    ) {
        return findValidToken(organizationId, purpose, rawToken, LocalDateTime.now());
    }

    private PasswordVerificationToken findValidToken(
            Long organizationId,
            PasswordVerificationPurpose purpose,
            String rawToken,
            LocalDateTime now
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApplicationException(ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND);
        }

        Organization organization = getOrganization(organizationId);
        PasswordVerificationToken token = passwordVerificationTokenRepository
                .findByOrganizationAndPurpose(organization, purpose)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.PASSWORD_VERIFICATION_TOKEN_NOT_FOUND
                ));

        if (token.getUsedAt() != null) {
            throw new ApplicationException(ErrorCode.PASSWORD_VERIFICATION_TOKEN_ALREADY_USED);
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new ApplicationException(ErrorCode.PASSWORD_VERIFICATION_TOKEN_EXPIRED);
        }
        if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
            throw new ApplicationException(ErrorCode.PASSWORD_VERIFICATION_TOKEN_INVALID);
        }

        return token;
    }

    private Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));
    }
}
