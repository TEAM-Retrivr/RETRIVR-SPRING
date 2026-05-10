package retrivr.retrivrspring.infrastructure.id;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.id.PublicIdGenerator;

@Component
@RequiredArgsConstructor
public class HmacPublicIdGenerator implements PublicIdGenerator {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final String COUPON_CODE_BASE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private final SecureRandom random = new SecureRandom();

  @Value("${public-id.secret}")
  private String secret;

  @Override
  public String generateRentalId(Long organizationId) {
    return "RNT-" + orgToken(organizationId) + "-" + randomBase(10, BASE62);
  }

  @Override
  public String generateItemId(Long organizationId) {
    return "ITM-" + orgToken(organizationId) + "-" + randomBase(10, BASE62);
  }

  @Override
  public String generateCouponCode() {
    return randomBase(4, COUPON_CODE_BASE) + "-" +
        randomBase(4, COUPON_CODE_BASE) + "-" +
        randomBase(4, COUPON_CODE_BASE);
  }

  // ===== org hash =====
  private String orgToken(Long organizationId) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
      mac.init(keySpec);

      byte[] digest = mac.doFinal(String.valueOf(organizationId).getBytes(StandardCharsets.UTF_8));
      return toHex(digest).substring(0, 6).toUpperCase();

    } catch (Exception e) {
      throw new IllegalStateException("org token 생성 실패", e);
    }
  }

  // ===== random =====
  private String randomBase(int length, String base) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(base.charAt(random.nextInt(base.length())));
    }
    return sb.toString();
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}