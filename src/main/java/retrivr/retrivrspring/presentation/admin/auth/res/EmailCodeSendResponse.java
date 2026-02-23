package retrivr.retrivrspring.presentation.admin.auth.res;

public record EmailCodeSendResponse(
        boolean success,
        int expiresInSeconds,
        String message
) {}
