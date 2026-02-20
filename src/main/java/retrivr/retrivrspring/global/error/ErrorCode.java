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

    //5000: Rental Error
    ILLEGAL_BORROWER_FIELD(HttpStatus.BAD_REQUEST, 5000, "대여자 정보 필드가 잘못되었습니다."),
    NOT_FOUND_RENTAL(HttpStatus.NOT_FOUND, 5001, "존재하지 않는 대여 정보입니다."),
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
