package retrivr.retrivrspring.global.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email-verification")
public class EmailVerificationProperties {

  private int expiresSeconds = 600;
  private long resendBlockSeconds = 60;
  private int maxFailedAttempts = 5;
}
