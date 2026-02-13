package retrivr.retrivrspring.global.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * CustomException Registration
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(ApplicationException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("[CustomException] {}", errorCode.getCode());
        log.warn("[CustomException] {}", e.getMessage());
        e.printStackTrace();

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, e.getMessage()));
    }
}
