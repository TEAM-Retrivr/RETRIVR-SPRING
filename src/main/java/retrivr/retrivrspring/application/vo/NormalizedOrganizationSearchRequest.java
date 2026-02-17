package retrivr.retrivrspring.application.vo;

import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

public record NormalizedOrganizationSearchRequest(
    String keyword,
    int size
) {

  private static final int DEFAULT_SIZE = 15;
  private static final int MAX_SIZE = 50;

  public static NormalizedOrganizationSearchRequest of(String keyword, Integer size) {
    String normalizedKeyword = normalizeKeyword(keyword);
    int normalizedSize = normalizeSize(size);
    return new NormalizedOrganizationSearchRequest(normalizedKeyword, normalizedSize);
  }

  public int sizePlusOne() {
    return size + 1;
  }

  private static String normalizeKeyword(String keyword) {
    if (keyword == null) {
      throw new ApplicationException(ErrorCode.NO_SEARCH_KEYWORD_EXCEPTION);
    }
    String trimmed = keyword.trim();
    if (trimmed.isBlank()) {
      throw new ApplicationException(ErrorCode.BLANK_SEARCH_KEYWORD_EXCEPTION);
    }
    return trimmed;
  }

  private static int normalizeSize(Integer size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return Math.min(size, MAX_SIZE);
  }
}