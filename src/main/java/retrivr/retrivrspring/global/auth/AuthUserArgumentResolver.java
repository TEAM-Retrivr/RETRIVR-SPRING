package retrivr.retrivrspring.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthOrg.class)
                && parameter.getParameterType().equals(AuthUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            org.springframework.web.context.request.NativeWebRequest webRequest,
            org.springframework.web.bind.support.WebDataBinderFactory binderFactory
    ) {

        HttpServletRequest request =
                webRequest.getNativeRequest(HttpServletRequest.class);

        Object authUser = request.getAttribute("authUser");
        if (authUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return authUser;
    }
}
