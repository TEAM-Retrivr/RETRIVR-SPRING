package retrivr.retrivrspring.global.error;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public enum ErrorCode {

    // 1000: Success Case
    SUCCESS(HttpStatus.OK, 1000, "정상적인 요청입니다."),
    CREATED(HttpStatus.CREATED, 1001, "정상적으로 생성되었습니다."),

    // 2000: Common Error
    INTERNAL_SERVER_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, 2000, "예기치 못한 오류가 발생했습니다."),
    NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, 2001, "존재하지 않는 리소스입니다."),
    INVALID_VALUE_EXCEPTION(HttpStatus.BAD_REQUEST, 2002, "올바르지 않은 요청 값입니다."),
    UNAUTHORIZED_EXCEPTION(HttpStatus.UNAUTHORIZED, 2003, "권한이 없는 요청입니다."),
    ALREADY_DELETE_EXCEPTION(HttpStatus.BAD_REQUEST, 2004, "이미 삭제된 리소스입니다."),
    FORBIDDEN_EXCEPTION(HttpStatus.FORBIDDEN, 2005, "인가되지 않는 요청입니다."),
    ALREADY_EXIST_EXCEPTION(HttpStatus.BAD_REQUEST, 2006, "이미 존재하는 리소스입니다."),
    SEARCH_LOG_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, 2007, "존재하지 않는 검색 로그입니다."),

    // 3000: Organization Error
    NO_SEARCH_KEYWORD_EXCEPTION(HttpStatus.BAD_REQUEST, 3000, "단체 찾기 요청 키워드가 없습니다."),
    BLANK_SEARCH_KEYWORD_EXCEPTION(HttpStatus.BAD_REQUEST, 3001, "단체 찾기 요청 키워드가 공백입니다."),
    DO_NOT_ENCODED_SEARCH_CURSOR(HttpStatus.INTERNAL_SERVER_ERROR, 3002, "커서 인코딩 중 문제가 발생했습니다."),
    INVALID_SEARCH_CURSOR(HttpStatus.BAD_REQUEST, 3003, "단체 찾기 요청 중 cursor 가 잘못되었습니다."),
    NOT_FOUND_ORGANIZATION(HttpStatus.NOT_FOUND, 3004, "존재하지 않는 단체입니다."),

    // 4000: Item Error
    NOT_FOUND_ITEM(HttpStatus.NOT_FOUND, 4000, "존재하지 않는 물건입니다."),
    NOT_FOUND_ITEM_UNIT(HttpStatus.NOT_FOUND, 4001, "존재하지 않는 물건 고유번호입니다."),
    NOT_AVAILABLE_ITEM(HttpStatus.SERVICE_UNAVAILABLE, 4002, "대여할 수 있는 수량이 없습니다."),
    NOT_AVAILABLE_ITEM_UNIT(HttpStatus.SERVICE_UNAVAILABLE, 4003, "해당 물건은 현재 대여 불가능합니다."),
    QUANTITY_CAN_NOT_BE_NEGATIVE(HttpStatus.BAD_REQUEST, 4004, "이용 가능한 수량은 음수가 될 수 없습니다."),
    ITEM_UNIT_DO_NOT_BELONG_TO_ITEM(HttpStatus.BAD_REQUEST, 4005, "요청된 물건에 속하지 않은 고유번호가 존재합니다."),

    //5000: Rental Error
    ILLEGAL_BORROWER_FIELD(HttpStatus.BAD_REQUEST, 5000, "대여자 정보 필드가 잘못되었습니다."),
    NOT_FOUND_RENTAL(HttpStatus.NOT_FOUND, 5001, "존재하지 않는 대여 정보입니다."),

    // 6000: Authentication Error
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, 6000, "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, 6001, "정지된 계정입니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 6002, "계정을 찾을 수 없습니다."),
    ACCOUNT_NOT_APPROVED(HttpStatus.NOT_FOUND, 6003, "계정이 비활성 상태입니다."),

    // 7000: Email Verification Error
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, 7000, "인증 요청이 존재하지 않습니다."),
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 7001, "인증 코드가 올바르지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, 7002, "인증 코드가 만료되었습니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, 7003, "이미 인증이 완료되었습니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, 7004, "존재하지 않는 이메일입니다."),

    // 7100: Signup Email Verification Error
    SIGNUP_EMAIL_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, 7100, "회원가입 인증 요청이 존재하지 않습니다."),
    SIGNUP_EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 7101, "회원가입 인증 코드가 올바르지 않습니다."),
    SIGNUP_EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, 7102, "회원가입 인증 코드가 만료되었습니다."),
    SIGNUP_EMAIL_CODE_ALREADY_USED(HttpStatus.BAD_REQUEST, 7103, "이미 사용된 회원가입 인증 코드입니다."),

    // 7200: Signup Token Error
    SIGNUP_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, 7200, "회원가입 토큰이 존재하지 않습니다."),
    SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, 7201, "회원가입 토큰이 유효하지 않습니다."),
    SIGNUP_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, 7202, "회원가입 토큰이 만료되었습니다."),
    SIGNUP_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, 7203, "이미 사용된 회원가입 토큰입니다."),

    // 8000: Password Reset Error
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, 8000, "비밀번호 재설정 토큰이 존재하지 않습니다."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, 8001, "비밀번호 재설정 토큰이 유효하지 않습니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, 8002, "비밀번호 재설정 토큰이 만료되었습니다."),
    PASSWORD_RESET_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, 8003, "이미 사용된 비밀번호 재설정 토큰입니다."),
    PASSWORD_RESET_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, 8004, "비밀번호와 비밀번호 확인 값이 일치하지 않습니다."),
    PASSWORD_RESET_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, 8005, "비밀번호 정책을 만족하지 않습니다."),
    ;

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, Integer code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
