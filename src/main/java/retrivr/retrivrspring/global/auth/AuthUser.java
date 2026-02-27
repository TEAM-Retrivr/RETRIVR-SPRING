package retrivr.retrivrspring.global.auth;

public record AuthUser(
        Long organizationId,
        String email
) {}