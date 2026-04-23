package retrivr.retrivrspring.presentation.open.auth.res;

public record PhoneVerificationVerifyResponse(
    String rawToken,
    String tokenId
) {

}
