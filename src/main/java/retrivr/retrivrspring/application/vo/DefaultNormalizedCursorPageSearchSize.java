package retrivr.retrivrspring.application.vo;

public record DefaultNormalizedCursorPageSearchSize(
    int size
) {

  private static final int DEFAULT_SIZE = 15;
  private static final int MAX_SIZE = 50;

  public static DefaultNormalizedCursorPageSearchSize of(Integer size) {
    int normalizedSize = normalizeSize(size);
    return new DefaultNormalizedCursorPageSearchSize(normalizedSize);
  }

  public int sizePlusOne() {
    return size + 1;
  }

  private static int normalizeSize(Integer size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return Math.min(size, MAX_SIZE);
  }
}
