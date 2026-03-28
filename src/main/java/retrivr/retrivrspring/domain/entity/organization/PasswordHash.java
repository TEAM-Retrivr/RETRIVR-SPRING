package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.global.error.DomainException;
import retrivr.retrivrspring.global.error.ErrorCode;

import java.util.regex.Pattern;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordHash {

    private static final Pattern PASSWORD_POLICY_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\p{Punct})(?!.*\\s).{8,}$"
    );

    @Column(name = "password_hash", nullable = false, length = 255)
    private String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    public static PasswordHash fromRawOrThrow(String rawPassword, PasswordEncoder passwordEncoder, ErrorCode errorCode) {
        if (rawPassword == null || !PASSWORD_POLICY_PATTERN.matcher(rawPassword).matches()) {
            throw new DomainException(errorCode);
        }

        return new PasswordHash(passwordEncoder.encode(rawPassword));
    }

    public static PasswordHash fromHashed(String hashedPassword) {
        if (hashedPassword == null) {
            return null;
        }

        String trimmed = hashedPassword.trim();
        if (trimmed.isEmpty()) {
            throw new DomainException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        return new PasswordHash(trimmed);
    }
}
