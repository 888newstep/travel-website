package travel.common.utils;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtHelperTest {
    @Test
    void shouldUseUtf8Base64KeyDerivationContract() {
        String secret = "测试-jwt-secret-that-is-long-enough-for-hmac-signing";
        byte[] expected = Base64.getEncoder().encode(secret.getBytes(StandardCharsets.UTF_8));

        assertArrayEquals(expected, JwtHelper.deriveSecretKey(secret).getEncoded());
        Jwts.builder().subject("contract-test").signWith(JwtHelper.deriveSecretKey(secret)).compact();
    }

    @Test
    void shouldRejectMissingOrShortSecrets() {
        assertThrows(IllegalStateException.class, () -> JwtHelper.deriveSecretKey(null));
        assertThrows(IllegalStateException.class, () -> JwtHelper.deriveSecretKey("short"));
    }
}
