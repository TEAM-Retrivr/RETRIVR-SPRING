package retrivr.retrivrspring.presentation.admin.auth.res;

public record PasswordResetSuccessResponse(
        boolean success
) {
    public static PasswordResetSuccessResponse ok() {
        return new PasswordResetSuccessResponse(true);
    }
}
