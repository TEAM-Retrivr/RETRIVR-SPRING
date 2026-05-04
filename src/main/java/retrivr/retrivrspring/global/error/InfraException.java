package retrivr.retrivrspring.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class InfraException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detail;

  public InfraException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.detail = "";
  }

  public InfraException(ErrorCode errorCode, String detail) {
    super(detail == null || detail.isBlank() ? errorCode.getMessage() : detail);
    this.errorCode = errorCode;
    this.detail = detail == null ? "" : detail;
  }

  public InfraException(ErrorCode errorCode, String detail, Throwable cause) {
    super(detail == null || detail.isBlank() ? errorCode.getMessage() : detail, cause);
    this.errorCode = errorCode;
    this.detail = detail == null ? "" : detail;
  }
}
