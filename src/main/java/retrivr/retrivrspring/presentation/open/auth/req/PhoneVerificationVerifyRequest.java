package retrivr.retrivrspring.presentation.open.auth.req;

import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;

public record PhoneVerificationVerifyRequest(
    String verificationId,
    PhoneVerificationPurpose purpose,
    String rawCode
) {

}
