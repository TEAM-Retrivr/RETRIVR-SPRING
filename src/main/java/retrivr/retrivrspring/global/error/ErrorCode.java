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
    BAD_REQUEST_EXCEPTION(HttpStatus.BAD_REQUEST, 2008, "잘못된 요청입니다."),
    UNSUPPORTED_NOTIFICATION_MESSAGE_TYPE(HttpStatus.INTERNAL_SERVER_ERROR, 2009, "지원하지 않는 알림 메시지 타입입니다."),

    // 3000: Organization Error
    NO_SEARCH_KEYWORD_EXCEPTION(HttpStatus.BAD_REQUEST, 3000, "단체 찾기 요청 키워드가 없습니다."),
    BLANK_SEARCH_KEYWORD_EXCEPTION(HttpStatus.BAD_REQUEST, 3001, "단체 찾기 요청 키워드가 공백입니다."),
    DO_NOT_ENCODED_SEARCH_CURSOR(HttpStatus.INTERNAL_SERVER_ERROR, 3002, "커서 인코딩 중 문제가 발생했습니다."),
    INVALID_SEARCH_CURSOR(HttpStatus.BAD_REQUEST, 3003, "단체 찾기 요청 중 cursor 가 잘못되었습니다."),
    NOT_FOUND_ORGANIZATION(HttpStatus.NOT_FOUND, 3004, "존재하지 않는 단체입니다."),
    ORGANIZATION_MISMATCH_EXCEPTION(HttpStatus.FORBIDDEN, 3005, "요청한 리소스가 해당 조직에 속하지 않습니다."),

    // 4000: Item Error
    NOT_FOUND_ITEM(HttpStatus.NOT_FOUND, 4000, "존재하지 않는 물건입니다."),
    NOT_FOUND_ITEM_UNIT(HttpStatus.NOT_FOUND, 4001, "존재하지 않는 물건 고유번호입니다."),
    NOT_AVAILABLE_ITEM(HttpStatus.SERVICE_UNAVAILABLE, 4002, "대여할 수 있는 수량이 없습니다."),
    NOT_AVAILABLE_ITEM_UNIT(HttpStatus.SERVICE_UNAVAILABLE, 4003, "해당 물건은 현재 대여 불가능합니다."),
    AVAILABLE_QUANTITY_UNDERFLOW_EXCEPTION(HttpStatus.BAD_REQUEST, 4004, "이용 가능한 수량은 음수가 될 수 없습니다."),
    AVAILABLE_QUANTITY_OVERFLOW_EXCEPTION(HttpStatus.BAD_REQUEST, 4005, "이용 가능한 수량은 총 수량을 넘을 수 없습니다."),
    ITEM_UNIT_DO_NOT_BELONG_TO_ITEM(HttpStatus.BAD_REQUEST, 4006, "요청된 물건에 속하지 않은 고유번호가 존재합니다."),
    ITEM_STATUS_TRANSITION_EXCEPTION(HttpStatus.BAD_REQUEST, 4007, "물건의 대여 상태를 요청된 상태로 변경할 수 없습니다."),
    INVALID_ITEM(HttpStatus.INTERNAL_SERVER_ERROR, 4008, "물건 데이터가 잘못되었습니다."),
    INVALID_ITEM_UNIT(HttpStatus.INTERNAL_SERVER_ERROR, 4009, "아이템 유닛 데이터 잘못되었습니다."),
    ITEM_UNIT_REQUIRED_FOR_UNIT_TYPE(HttpStatus.BAD_REQUEST, 4010, "UNIT 타입 물건은 고유번호(ItemUnit)가 반드시 필요합니다."),
    ITEM_UNIT_NOT_ALLOWED_FOR_NON_UNIT_TYPE(HttpStatus.BAD_REQUEST, 4011, "Non Unit 타입 물건은 고유번호를 사용할 수 없습니다."),
    CANNOT_CONVERT_NON_UNIT_ITEM_WITH_UNAVAILABLE_QUANTITY_TO_UNIT(HttpStatus.BAD_REQUEST, 4012, "대여 불가능 수량이 있는 비유닛 물품은 유닛 물품으로 변경할 수 없습니다."),
    ITEM_PUBLIC_ID_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 4013, "아이템 공개 ID 생성에 실패했습니다."),

    //5000: Rental Error
    ILLEGAL_BORROWER_LABEL(HttpStatus.BAD_REQUEST, 5000, "대여자 라벨이 잘못되었습니다."),
    NOT_FOUND_RENTAL(HttpStatus.NOT_FOUND, 5001, "존재하지 않는 대여 정보입니다."),
    RENTAL_STATUS_TRANSITION_EXCEPTION(HttpStatus.BAD_REQUEST, 5002, "대여 상태를 요청된 상태로 변경할 수 없습니다."),
    INVALID_RENTAL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, 5003, "대여 데이터가 잘못되었습니다."),
    RENTAL_DUE_DATE_UPDATE_EXCEPTION(HttpStatus.BAD_REQUEST, 5004, "반납 일자를 수정할 수 없는 상태입니다."),
    DO_NOT_SEND_OVERDUE_MESSAGE(HttpStatus.CONFLICT, 5005, "연체 문자를 보낼 수 없는 대여 상태입니다."),
    RENTAL_PUBLIC_ID_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 5006, "대여 공개 ID 생성에 실패했습니다."),

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
    EMAIL_VERIFICATION_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, 7104, "인증 코드는 60초 후 재요청 가능합니다."),

    // 7200: Signup Token Error
    SIGNUP_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, 7200, "회원가입 토큰이 존재하지 않습니다."),
    SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, 7201, "회원가입 토큰이 유효하지 않습니다."),
    SIGNUP_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, 7202, "회원가입 토큰이 만료되었습니다."),
    SIGNUP_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, 7203, "이미 사용된 회원가입 토큰입니다."),

    // 7300: Admin Code Error
    ADMIN_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 7301, "관리자 코드가 일치하지 않습니다."),
    NOT_FOUND_ADMIN_CODE_VERIFICATION_TOKEN(HttpStatus.FORBIDDEN, 7302, "관리자 코드 인증 토큰이 존재하지 않습니다."),
    EXPIRED_ADMIN_CODE_VERIFICATION_TOKEN(HttpStatus.FORBIDDEN, 7303, "관리자 코드 인증 토큰이 만료되었습니다."),
    ADMIN_CODE_VERIFICATION_TOKEN_MISMATCH(HttpStatus.BAD_REQUEST, 7304, "관리자 코드 인증 토큰이 일치하지 않습니다."),
    ALREADY_USED_ADMIN_CODE_VERIFICATION_TOKEN(HttpStatus.FORBIDDEN, 7305, "이미 사용된 관리자 코드 인증 토큰입니다."),

    // 7400: Phone Verification Error
    TOO_MANY_PHONE_VERIFICATION_REQUEST(HttpStatus.TOO_MANY_REQUESTS, 7400, "핸드폰 번호 인증 요청이 너무 많습니다."),
    NOT_FOUND_PHONE_VERIFICATION(HttpStatus.NOT_FOUND, 7401, "핸드폰 번호 인증 객체가 없습니다."),
    PHONE_VERIFICATION_PURPOSE_MISMATCH(HttpStatus.BAD_REQUEST, 7402, "핸드폰 번호 인증 목적이 일치하지 않습니다."),
    EXPIRED_PHONE_VERIFICATION(HttpStatus.BAD_REQUEST, 7403, "핸드폰 번호 인증 시간이 만료되었습니다."),
    PHONE_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 7404, "인증번호가 일치하지 않습니다."),
    NOT_FOUND_PHONE_VERIFICATION_TOKEN(HttpStatus.NOT_FOUND, 7405, "핸드폰 번호 인증 토큰이 없습니다."),
    PHONE_VERIFICATION_TOKEN_MISMATCH(HttpStatus.BAD_REQUEST, 7406, "인증 토큰이 일치하지 않습니다."),
    EXPIRED_PHONE_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, 7407, "인증 토큰이 만료되었습니다."),

    // 8000: Password Reset Error
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, 8000, "비밀번호 재설정 토큰이 존재하지 않습니다."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, 8001, "비밀번호 재설정 토큰이 유효하지 않습니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, 8002, "비밀번호 재설정 토큰이 만료되었습니다."),
    PASSWORD_RESET_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, 8003, "이미 사용된 비밀번호 재설정 토큰입니다."),
    PASSWORD_RESET_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, 8004, "비밀번호와 비밀번호 확인 값이 일치하지 않습니다."),
    PASSWORD_RESET_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, 8005, "비밀번호 정책을 만족하지 않습니다."),

    // 9000: Return Event Error
    RETURN_EVENT_CAN_NOT_CREATE(HttpStatus.INTERNAL_SERVER_ERROR, 9000, "반납 이벤트를 생성할 수 없습니다."),

    // 10000: Borrower Error
    INVALID_PHONE_NUMBER_EXCEPTION(HttpStatus.BAD_REQUEST, 10000, "유효하지 않은 전화번호입니다."),

    // 11000: File Storage Error
    NOT_ALLOWED_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, 11000, "저장할 수 없는 이미지 컨텐츠 유형입니다."),
    NOT_FOUND_PROFILE_IMAGE(HttpStatus.NOT_FOUND, 11001, "ObjectKey 에 대한 이미지를 찾을 수 없습니다."),

    // 11100: S3 Storage Error
    S3_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 11100, "S3 스토리지 오류입니다."),
    EXTENSION_MUST_NOT_BE_BLANK(HttpStatus.BAD_REQUEST, 11101, "저장할 이미지의 확장자가 없습니다."),
    UNSUPPORTED_EXTENSION(HttpStatus.BAD_REQUEST, 11102, "해당 확장자를 가진 이미지 저장이 불가합니다."),

    // 11200: BizMsg Infra Error
    BIZMSG_API_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 11200, "비즈엠 API 요청에 실패했습니다."),
    BIZMSG_API_EMPTY_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, 11201, "비즈엠 API 응답이 비어 있습니다."),
    BIZMSG_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 11202, "비즈엠 알림톡 발송에 실패했습니다."),
    BIZMSG_TEMPLATE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, 11203, "비즈엠 알림톡 템플릿 정보가 올바르지 않습니다."),
    MESSAGE_SENDER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, 11204, "메시지 채널 발송기를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, Integer code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
