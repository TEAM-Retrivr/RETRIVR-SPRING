package retrivr.retrivrspring.infrastructure.image;

import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageContentTypePolicy {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg",
      "image/jpg",
      "image/png",
      "image/webp"
  );

  public static boolean isAllowed(String contentType) {
    return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
  }

  public static String extractExtension(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      throw new IllegalArgumentException("content type must not be blank");
    }
    return switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> "jpeg";
      case "image/jpg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> throw new IllegalArgumentException("unsupported content type: " + contentType);
    };
  }
}
