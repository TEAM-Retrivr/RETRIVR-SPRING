package retrivr.retrivrspring.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InfraException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detail;

  public InfraException(ErrorCode errorCode) {
    this.errorCode = errorCode;
    this.detail = "";
  }
}
