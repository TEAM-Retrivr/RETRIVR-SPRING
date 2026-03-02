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
        emailService.sendTextEmail(
                toEmail,
                buildSubject(purpose),
                buildVerificationMessage(purpose, rawCode, expiresInSeconds)
        );
    }

    private String buildSubject(EmailVerificationPurpose purpose) {
        if (purpose == EmailVerificationPurpose.SIGNUP) {
            return "[RETRIVR] 회원가입 자격 증명 확인";
        }
        if (purpose == EmailVerificationPurpose.PASSWORD_RESET) {
            return "[RETRIVR] 비밀번호 재설정 자격 증명 확인";
        }
        if (purpose == EmailVerificationPurpose.LOGIN) {
            return "[RETRIVR] 로그인 자격 증명 확인";
        }
        return "[RETRIVR] 자격 증명 확인";
    }

    private String buildVerificationMessage(
            EmailVerificationPurpose purpose,
            String rawCode,
            int expiresInSeconds
    ) {
        String actionLabel = toActionLabel(purpose);
        String expireText = formatExpireText(expiresInSeconds);

        return "자격 증명 확인\n"
                + "안녕하십니까,\n\n"
                + "RETRIVR 계정에서 " + actionLabel + " 요청이 확인되었습니다. "
                + actionLabel + " 요청을 시작하신 경우 다음 코드를 입력하여 "
                + "자격 증명을 확인하고 절차를 완료해 주세요.\n\n"
                + "확인 코드\n"
                + rawCode + "\n"
                + "(이 코드는 전송 " + expireText + " 후에 만료됩니다.)\n\n"
                + buildSecurityMessage(purpose);
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
            return "본인이 요청하신 내역이 아니라면 계정 보안을 위해 비밀번호를 즉시 변경해 주시기 바랍니다.";
        }
        return "본인이 요청하신 내역이 아니라면 이 메일을 무시하시기 바랍니다.";
    }

    private String formatExpireText(int expiresInSeconds) {
        if (expiresInSeconds % 60 == 0) {
            return (expiresInSeconds / 60) + "분";
        }
        return expiresInSeconds + "초";
    }
}
