package retrivr.retrivrspring.infrastructure.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationCodeSender;
import retrivr.retrivrspring.application.service.email.EmailService;
import retrivr.retrivrspring.domain.entity.organization.enumerate.EmailVerificationPurpose;

@Component
@RequiredArgsConstructor
public class EmailVerificationCodeSenderImpl implements EmailVerificationCodeSender {

    private final EmailService emailService;

    @Override
    public void sendVerificationCode(
            String toEmail,
            String rawCode,
            EmailVerificationPurpose purpose,
            int expiresInSeconds
    ) {
        emailService.sendHtmlEmail(
                toEmail,
                buildSubject(purpose),
                buildVerificationHtml(purpose, rawCode, expiresInSeconds)
        );
    }

    private String buildSubject(EmailVerificationPurpose purpose) {
        if (purpose == EmailVerificationPurpose.SIGNUP) {
            return "[RETRIVR] 회원가입 인증 코드";
        }
        if (purpose == EmailVerificationPurpose.PASSWORD_RESET) {
            return "[RETRIVR] 비밀번호 재설정 인증 코드";
        }
        if (purpose == EmailVerificationPurpose.LOGIN) {
            return "[RETRIVR] 로그인 인증 코드";
        }
        return "[RETRIVR] 인증 코드";
    }

    private String buildVerificationHtml(
            EmailVerificationPurpose purpose,
            String rawCode,
            int expiresInSeconds
    ) {
        String actionLabel = toActionLabel(purpose);
        String expireText = formatExpireText(expiresInSeconds);

        return "<!doctype html>"
                + "<html lang=\"ko\">"
                + "<body style=\"margin:0;padding:0;background:#ffffff;font-family:'Pretendard','Apple SD Gothic Neo','Malgun Gothic',Arial,sans-serif;color:#2D4E7F;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#ffffff;\">"
                + "<tr><td align=\"center\" style=\"padding:40px 16px;\">"
                + "<table role=\"presentation\" width=\"560\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:560px;background:#ffffff;border:1px solid #CDD6DE;border-radius:12px;\">"
                + "<tr><td style=\"padding:28px 24px 18px 24px;text-align:center;font-size:22px;font-weight:700;color:#ffffff;background:linear-gradient(135deg,#7FB5F7 0%,#CDD6DE 100%);border-radius:12px 12px 0 0;\">"
                + actionLabel + " 인증 안내"
                + "</td></tr>"
                + "<tr><td style=\"padding:22px 24px 10px 24px;text-align:center;font-size:15px;line-height:1.7;color:#2D4E7F;\">"
                + "요청하신 " + actionLabel + "을 진행하려면 아래 인증번호를 입력해 주세요."
                + "</td></tr>"
                + "<tr><td style=\"padding:20px 24px 12px 24px;text-align:center;\">"
                + "<div style=\"display:inline-block;background:#ffffff;border:2px solid #76ADFF;border-radius:10px;padding:16px 28px;font-size:40px;font-weight:800;letter-spacing:8px;line-height:1;color:#2D4E7F;\">"
                + rawCode
                + "</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:0 24px 24px 24px;text-align:center;font-size:13px;color:#7998C5;\">"
                + "인증번호는 " + expireText + " 동안 유효합니다."
                + "</td></tr>"
                + "<tr><td style=\"padding:0 24px 28px 24px;text-align:center;font-size:13px;line-height:1.6;color:#7998C5;\">"
                + buildSecurityMessage(purpose)
                + "</td></tr>"
                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    private String toActionLabel(EmailVerificationPurpose purpose) {
        if (purpose == EmailVerificationPurpose.SIGNUP) {
            return "회원가입";
        }
        if (purpose == EmailVerificationPurpose.PASSWORD_RESET) {
            return "비밀번호 재설정";
        }
        if (purpose == EmailVerificationPurpose.LOGIN) {
            return "로그인";
        }
        return "이메일 인증";
    }

    private String buildSecurityMessage(EmailVerificationPurpose purpose) {
        if (purpose == EmailVerificationPurpose.PASSWORD_RESET) {
            return "본인이 요청하지 않았다면 계정 보안을 위해 비밀번호를 즉시 변경해 주세요.";
        }
        return "본인이 요청하지 않았다면 이 메일을 무시해 주세요.";
    }

    private String formatExpireText(int expiresInSeconds) {
        if (expiresInSeconds % 60 == 0) {
            return (expiresInSeconds / 60) + "분";
        }
        return expiresInSeconds + "초";
    }
}
