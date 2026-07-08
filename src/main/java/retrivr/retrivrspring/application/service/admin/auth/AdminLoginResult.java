package retrivr.retrivrspring.application.service.admin.auth;

public record AdminLoginResult(
        Long organizationId,
        String email,
        String accessToken,
        String refreshToken
) {}
