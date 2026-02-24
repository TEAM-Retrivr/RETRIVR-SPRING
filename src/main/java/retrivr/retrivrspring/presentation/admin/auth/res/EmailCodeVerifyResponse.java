package retrivr.retrivrspring.presentation.admin.auth.res;

public record EmailCodeVerifyResponse(
        String signupToken,
        int expiresInSeconds
) {}