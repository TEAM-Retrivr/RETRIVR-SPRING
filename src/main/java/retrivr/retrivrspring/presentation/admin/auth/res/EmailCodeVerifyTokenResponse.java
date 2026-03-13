package retrivr.retrivrspring.presentation.admin.auth.res;

public record EmailCodeVerifyTokenResponse(
        String tokenType,
        String token,
        int expiresInSeconds
) {
    public static EmailCodeVerifyTokenResponse signupToken(String token, int expiresInSeconds) {
        return new EmailCodeVerifyTokenResponse("SIGNUP", token, expiresInSeconds);
    }

    public static EmailCodeVerifyTokenResponse passwordResetToken(String token, int expiresInSeconds) {
        return new EmailCodeVerifyTokenResponse("PASSWORD_RESET", token, expiresInSeconds);
    }

    public static EmailCodeVerifyTokenResponse emailChangeToken(String token, int expiresInSeconds) {
        return new EmailCodeVerifyTokenResponse("EMAIL_CHANGE", token, expiresInSeconds);
    }
}
