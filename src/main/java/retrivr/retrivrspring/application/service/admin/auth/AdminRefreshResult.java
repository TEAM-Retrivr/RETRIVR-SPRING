package retrivr.retrivrspring.application.service.admin.auth;

public record AdminRefreshResult(
        Long organizationId,
        String email,
        String accessToken
) {}
