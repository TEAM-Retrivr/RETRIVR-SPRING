package retrivr.retrivrspring.global.auth.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ManagerAuthInterceptor implements HandlerInterceptor {

  @Value("${manager-secret}")
  private String managerSecret;

  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler
  ) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    boolean required = handlerMethod.hasMethodAnnotation(ValidManager.class);

    if (!required) {
      return true;
    }

    String managerToken = request.getHeader("X-Manager-Token");

    if (managerToken == null || managerToken.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "매니저 토큰이 없습니다.");
    }

    if (!managerToken.equals(managerSecret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "매니저 토큰이 잘못되었습니다.");
    }

    return true;
  }
}
