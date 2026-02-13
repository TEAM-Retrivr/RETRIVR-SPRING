package retrivr.retrivrspring.global.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ErrorResponse {

  private final HttpStatus status;
  private final Integer code;
  private final String message;
  private final String detail;

  public static ErrorResponse of(ErrorCode errorCode, String detail) {
    return ErrorResponse.builder()
        .status(errorCode.getHttpStatus())
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .detail(detail)
        .build();
  }

  public static ErrorResponse of(ErrorCode errorCode) {
    return of(errorCode, "");
  }
}