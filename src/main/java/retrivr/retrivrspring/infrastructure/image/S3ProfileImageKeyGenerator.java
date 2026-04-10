package retrivr.retrivrspring.infrastructure.image;

import java.util.UUID;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.image.ProfileImageKeyGeneratorPort;

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
      throw new IllegalArgumentException("extension must not be blank");
    }

    String value = extension.toLowerCase().replace(".", "");

    return switch (value) {
      case "jpg", "jpeg", "png", "webp" -> value;
      default -> throw new IllegalArgumentException("unsupported extension: " + extension);
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
