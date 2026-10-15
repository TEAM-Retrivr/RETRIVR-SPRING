package retrivr.retrivrspring.global.error;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleVersionConflict(Exception e) {
        ErrorCode errorCode = ErrorCode.VERSION_CONFLICT;
        log.warn("[VersionConflict] message={}", e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, ""));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        log.warn("[InvalidRequest] message={}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(ErrorCode.BAD_REQUEST_EXCEPTION, "요청 값의 형식이 올바르지 않습니다."));
    }

    /**
     * CustomException Registration
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("[ApplicationException] code={}, message={}", errorCode.getCode(), errorCode.getMessage(), e);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, e.getDetail()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("[DomainException] code={}, message={}", errorCode.getCode(), errorCode.getMessage(), e);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, e.getDetail()));
    }

    @ExceptionHandler(InfraException.class)
    public ResponseEntity<ErrorResponse> handleInfraException(InfraException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("[InfraException] code={}, message={}", errorCode.getCode(), errorCode.getMessage(), e);
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, e.getDetail()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("잘못된 요청입니다.");

        log.error("[MethodArgumentNotValidException] message={}", message, e);
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(ErrorCode.BAD_REQUEST_EXCEPTION, message));
    }
}
