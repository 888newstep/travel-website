package travel.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Base64;

public class JwtHelper {
    // 浠ょ墝杩囨湡鏃堕棿锛?4灏忔椂
    private static long tokenExpiration = 24 * 60 * 60 * 1000;

    private static String getTokenSignKey() {
        String systemProperty = System.getProperty("jwt.secret");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        String envVariable = System.getenv("JWT_SECRET");
        if (envVariable != null && !envVariable.isBlank()) {
            return envVariable;
        }
        throw new IllegalStateException("Property 'jwt.secret' must be configured before using JwtHelper");
    }
    // 鐢熸垚瀵嗛挜瀵硅薄
    private static SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getEncoder().encode(getTokenSignKey().getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 鐢熸垚token瀛楃涓?     * @param userId 鐢ㄦ埛ID
     * @param userType 鐢ㄦ埛绫诲瀷
     * @return 鐢熸垚鐨則oken瀛楃涓?     */
    public static String createToken(Long userId, Integer userType) {
        String token = Jwts.builder()
                .subject("TRAVEL-PLATFORM-USER")
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .issuedAt(new Date())
                .id(String.valueOf(userId))
                .claim("userId", userId)
                .claim("userType", userType)
                .signWith(getSecretKey())
                .compact();
        return token;
    }

    /**
     * 浠巘oken瀛楃涓茶幏鍙杣serid
     * @param token token瀛楃涓?     * @return 鐢ㄦ埛ID
     */
    public static Long getUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jwt<JwsHeader, Claims> jwt = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jwt.getPayload();
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            return null;
        } catch (Exception e) {
            // 澶勭悊token瑙ｆ瀽寮傚父
            return null;
        }
    }

    /**
     * 浠巘oken瀛楃涓茶幏鍙杣serType
     * @param token token瀛楃涓?     * @return 鐢ㄦ埛绫诲瀷
     */
    public static Integer getUserType(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jwt<JwsHeader, Claims> jwt = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jwt.getPayload();
            return (Integer) claims.get("userType");
        } catch (Exception e) {
            // 澶勭悊token瑙ｆ瀽寮傚父
            return null;
        }
    }

    /**
     * 浠巘oken瀛楃涓茶幏鍙杣serName
     * @param token token瀛楃涓?     * @return 鐢ㄦ埛鍚?     */
    public static String getUserName(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        try {
            Jwt<JwsHeader, Claims> jwt = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jwt.getPayload();
            return (String) claims.get("userName");
        } catch (Exception e) {
            // 澶勭悊token瑙ｆ瀽寮傚父
            return "";
        }
    }

    /**
     * 鍒ゆ柇token鏄惁鏈夋晥
     * @param token token瀛楃涓?     * @return 鏄惁杩囨湡
     */
    public static boolean isExpiration(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        try {
            Jwt<JwsHeader, Claims> jwt = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jwt.getPayload();
            boolean isExpire = claims.getExpiration().before(new Date());
            // 娌℃湁杩囨湡锛屾湁鏁堬紝杩斿洖false
            return isExpire;
        } catch (Exception e) {
            // 杩囨湡鎴栬В鏋愬紓甯革紝杩斿洖true
            return true;
        }
    }

    /**
     * 鍒锋柊Token
     * @param token 鏃oken
     * @return 鏂皌oken
     */
    public static String refreshToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jwt<JwsHeader, Claims> jwt = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jwt.getPayload();
            // 浣跨敤claims鍙橀噺鑾峰彇鐢ㄦ埛淇℃伅锛岄伩鍏嶉噸澶嶈В鏋恡oken
            Object userIdObj = claims.get("userId");
            Long userId = null;
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            }
            Integer userType = (Integer) claims.get("userType");
            if (userId != null && userType != null) {
                return createToken(userId, userType);
            }
            return null;
        } catch (Exception e) {
            // 澶勭悊token瑙ｆ瀽寮傚父
            return null;
        }
    }

    /**
     * 楠岃瘉token鏄惁鏈夋晥
     * @param token token瀛楃涓?     * @return 鏄惁鏈夋晥
     */
    public static boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // token鏃犳晥
            return false;
        }
    }

    /**
     * 浠巘oken瀛楃涓茶幏鍙栬繃鏈熸椂闂?     * @param token token瀛楃涓?     * @return 杩囨湡鏃堕棿鎴?     */
    public static Long getExpiration(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jwt<JwsHeader, Claims> jwt = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jwt.getPayload();
            return claims.getExpiration().getTime();
        } catch (Exception e) {
            // 澶勭悊token瑙ｆ瀽寮傚父
            return null;
        }
    }

    /**
     * 涓绘柟娉曠敤浜庢祴璇?     * @param args 鍙傛暟
     */
    public static void main(String[] args) {
        String token = createToken(1L, 1);
        System.out.println("鐢熸垚鐨則oken: " + token);
        System.out.println("浠巘oken鑾峰彇userId: " + getUserId(token));
        System.out.println("浠巘oken鑾峰彇userType: " + getUserType(token));
        System.out.println("token鏄惁鏈夋晥: " + !isExpiration(token));
        System.out.println("楠岃瘉token: " + validateToken(token));
        System.out.println("鍒锋柊token: " + refreshToken(token));
    }
}


