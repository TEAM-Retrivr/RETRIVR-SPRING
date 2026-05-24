package retrivr.retrivrspring.presentation.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.web.CorsPolicyProperties;

@Component
@RequiredArgsConstructor
public class AdminAuthOriginValidator {

    private final CorsPolicyProperties corsPolicyProperties;

    public void validate(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_EXCEPTION, "Origin header is required.");
        }

        if (!corsPolicyProperties.isAllowedOrigin(origin)) {
            throw new ApplicationException(ErrorCode.FORBIDDEN_EXCEPTION, "Origin is not allowed.");
        }
    }
}
