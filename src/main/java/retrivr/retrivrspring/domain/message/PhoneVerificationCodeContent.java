package retrivr.retrivrspring.domain.message;

public class PhoneVerificationCodeContent extends MessageContent {

  private static final String TEMPLATE_CODE = "rtr-v1-auth-code";

  private final String verificationCode;
  private final int expirationMinutes;

  public PhoneVerificationCodeContent(String verificationCode, int expirationMinutes) {
    this.verificationCode = verificationCode;
    this.expirationMinutes = expirationMinutes;
  }

  @Override
  protected String buildSubject() {
    return "[Retrivr] 전화번호 인증번호를 확인해주세요.";
  }

  @Override
  protected String buildTemplateCode() {
    return TEMPLATE_CODE;
  }

  @Override
  protected String buildTemplateTitle() {
    return verificationCode;
  }

  @Override
  protected String buildTemplateBody() {
    return String.join("\n",
        "*" + expirationMinutes + "분 이내에 입력해주세요.",
        "타인에게 노출되지 않도록 주의해주세요."
    );
  }

  @Override
  protected String buildBody() {
    return String.format(
        "[Retrivr]%n%n인증번호 %s 입니다.%n%n%d분 이내에 입력해주세요.%n타인에게 노출되지 않도록 주의해주세요.",
        verificationCode,
        expirationMinutes
    );
  }
}
