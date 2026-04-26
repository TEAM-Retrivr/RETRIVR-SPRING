package retrivr.retrivrspring.infrastructure.image;

import java.util.UUID;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.image.ProfileImageKeyGeneratorPort;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.error.InfraException;

@Component
public class S3ProfileImageKeyGenerator implements ProfileImageKeyGeneratorPort {

  @Override
  public String generate(Long organizationId, String extension) {
    String normalizedExtension = normalizeExtension(extension);
    return "organizations/%d/profile/%s.%s"
        .formatted(organizationId, UUID.randomUUID(), normalizedExtension);
  }

  private String normalizeExtension(String extension) {
    if (extension == null || extension.isBlank()) {
      throw new InfraException(ErrorCode.EXTENSION_MUST_NOT_BE_BLANK);
    }

    String value = extension.toLowerCase().replace(".", "");

    return switch (value) {
      case "jpg", "jpeg", "png", "webp" -> value;
      default -> throw new InfraException(ErrorCode.UNSUPPORTED_EXTENSION, "unsupported extension: " + extension);
    };
  }

  @Override
  public boolean isProfileImageKeyOwner(Long organizationId, String objectKey) {
    if (organizationId == null || objectKey == null || objectKey.isBlank()) {
      return false;
    }

    String expectedPrefix = "organizations/" + organizationId + "/profile/";

    if (!objectKey.startsWith(expectedPrefix)) {
      return false;
    }

    String[] parts = objectKey.split("/");
    if (parts.length != 4) {
      return false;
    }

    return true;
  }
}
