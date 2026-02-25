package retrivr.retrivrspring.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import retrivr.retrivrspring.global.config.JwtTokenProvider;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        String path = request.getRequestURI();

        // 로그인/회원가입은 필터 통과
        if (path.startsWith("/api/public/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/admin/v1/auth/signup/**")
                || path.startsWith("/api/admin/v1/auth/")
                || path.startsWith("/api/public/**")) {
            filterChain.doFilter(request, response);
            return;
        }


        String bearerToken = request.getHeader("Authorization");
        String token = jwtTokenProvider.resolveToken(bearerToken);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        System.out.println("VALID: true");

        AuthUser authUser = jwtTokenProvider.getAuthUser(token);

        request.setAttribute("authUser", authUser);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}