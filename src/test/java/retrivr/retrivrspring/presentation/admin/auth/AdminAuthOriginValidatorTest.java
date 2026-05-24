package retrivr.retrivrspring.presentation.admin.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.web.CorsPolicyProperties;

class AdminAuthOriginValidatorTest {

    private AdminAuthOriginValidator validator;

    @BeforeEach
    void setUp() {
        CorsPolicyProperties corsPolicyProperties = new CorsPolicyProperties();
        corsPolicyProperties.setAllowedOrigins(List.of("https://retrivr-web.vercel.app"));
        validator = new AdminAuthOriginValidator(corsPolicyProperties);
    }

    @Test
    void validate_allowsConfiguredOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ORIGIN, "https://retrivr-web.vercel.app");

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void validate_rejectsMissingOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> validator.validate(request)
        );

        assertEquals(ErrorCode.FORBIDDEN_EXCEPTION, exception.getErrorCode());
    }

    @Test
    void validate_rejectsUnconfiguredOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ORIGIN, "https://www.retrivr.kr");

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> validator.validate(request)
        );

        assertEquals(ErrorCode.FORBIDDEN_EXCEPTION, exception.getErrorCode());
    }
}
