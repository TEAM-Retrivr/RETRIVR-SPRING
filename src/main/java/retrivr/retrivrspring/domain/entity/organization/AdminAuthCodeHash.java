package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuthCodeHash {

    @Column(name = "admin_code_hash", nullable = false, length = 255)
    private String value;

    private AdminAuthCodeHash(String value) {
        this.value = value;
    }

    public static AdminAuthCodeHash fromRawOrThrow(String rawAdminCode, PasswordEncoder passwordEncoder) {
        if (rawAdminCode == null) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        String trimmed = rawAdminCode.trim();
        if (trimmed.isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        return new AdminAuthCodeHash(passwordEncoder.encode(trimmed));
    }

    public static AdminAuthCodeHash fromHashed(String hashedAdminCode) {
        if (hashedAdminCode == null) {
            return null;
        }

        String trimmed = hashedAdminCode.trim();
        if (trimmed.isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_VALUE_EXCEPTION);
        }

        return new AdminAuthCodeHash(trimmed);
    }
}
