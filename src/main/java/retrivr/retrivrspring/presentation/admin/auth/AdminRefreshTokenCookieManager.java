package retrivr.retrivrspring.presentation.admin.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AdminRefreshTokenCookieManager {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/";

    @Value("${auth.refresh-token.cookie-secure}")
    private boolean refreshTokenCookieSecure;

    @Value("${auth.refresh-token.cookie-same-site}")
    private String refreshTokenCookieSameSite;

    @Value("${jwt.refresh-token.expire-time}")
    private long refreshTokenExpireTimeMillis;

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(refreshTokenExpireTimeMillis / 1000)
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    public String extract(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
