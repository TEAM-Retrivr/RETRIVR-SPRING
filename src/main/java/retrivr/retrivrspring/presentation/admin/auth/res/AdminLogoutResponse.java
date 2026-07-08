package retrivr.retrivrspring.presentation.admin.auth.res;

public record AdminLogoutResponse(
        boolean success
) {
    public static AdminLogoutResponse ok() {
        return new AdminLogoutResponse(true);
    }
}
