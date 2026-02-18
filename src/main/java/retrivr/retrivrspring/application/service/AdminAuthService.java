package retrivr.retrivrspring.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.organization.OrganizationStatus;
import retrivr.retrivrspring.domain.repository.OrganizationRepository;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminLoginRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminLoginResponse;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminLoginResponse login(AdminLoginRequest request) {
        // 1. 이메일로 Organization 조회
        Organization org = organizationRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 계정 상태 확인
        if (org.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new ApplicationException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), org.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 마지막 로그인 시간 업데이트
        org.updateLastLoginAt(LocalDateTime.now());
        organizationRepository.save(org);

        // 5. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(org.getId(), org.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(org.getId(), org.getEmail());

        // 6. 응답 반환
        return new AdminLoginResponse(org.getId(), org.getEmail(), accessToken, refreshToken);
    }
}