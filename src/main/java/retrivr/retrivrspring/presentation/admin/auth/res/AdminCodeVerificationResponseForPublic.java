package retrivr.retrivrspring.presentation.admin.auth.res;

public record AdminCodeVerificationResponseForPublic(
    String rawToken,
    Long rentalId
) {

}
