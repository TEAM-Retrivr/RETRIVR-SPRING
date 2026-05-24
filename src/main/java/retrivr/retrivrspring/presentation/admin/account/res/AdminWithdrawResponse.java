package retrivr.retrivrspring.presentation.admin.account.res;

public record AdminWithdrawResponse(
        boolean success
) {
    public static AdminWithdrawResponse ok() {
        return new AdminWithdrawResponse(true);
    }
}
