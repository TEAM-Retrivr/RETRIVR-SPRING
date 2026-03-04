package retrivr.retrivrspring.application.service.admin.auth;

import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;

public interface EmailVerificationCodeSender {

    void sendVerificationCode(String toEmail, String rawCode, EmailVerificationPurpose purpose, int expiresInSeconds);
}
