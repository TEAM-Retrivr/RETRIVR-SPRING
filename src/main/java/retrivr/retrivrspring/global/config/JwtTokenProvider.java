package retrivr.retrivrspring.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.global.auth.AuthUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expire-time}")
    private long accessTokenExpireTime;

    @Value("${jwt.refresh-token.expire-time}")
    private long refreshTokenExpireTime;

    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey =
                Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long organizationId, String email) {
        // JWT 생성 로직
        return Jwts.builder()
                .setSubject(String.valueOf(organizationId))
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpireTime))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Long organizationId, String email) {
        // Refresh token 생성 로직
        return Jwts.builder()
                .setSubject(String.valueOf(organizationId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpireTime))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public AuthUser getAuthUser(String token) {
        Claims claims = getClaims(token);

        Long organizationId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);

        return new AuthUser(organizationId, email);
    }

    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
