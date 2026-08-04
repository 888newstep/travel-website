package travel.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Base64;

public class JwtHelper {
    private static final String DEFAULT_JWT_SECRET = "dev-only-jwt-secret-change-me-before-production-32bytes";

    // 令牌过期时间：24小时
    private static long tokenExpiration = 24 * 60 * 60 * 1000;

    private static String getTokenSignKey() {
        String systemProperty = System.getProperty("jwt.secret");
        if (systemProperty != null && !systemProperty.isEmpty()) {
            return systemProperty;
        }
        String envVariable = System.getenv("JWT_SECRET");
        if (envVariable != null && !envVariable.isEmpty()) {
            return envVariable;
        }
        return DEFAULT_JWT_SECRET;
    }
    // 生成密钥对象
    private static SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getEncoder().encode(getTokenSignKey().getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成token字符串
     * @param userId 用户ID
     * @param userType 用户类型
     * @return 生成的token字符串
     */
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
     * 从token字符串获取userid
     * @param token token字符串
     * @return 用户ID
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
            // 处理token解析异常
            return null;
        }
    }

    /**
     * 从token字符串获取userType
     * @param token token字符串
     * @return 用户类型
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
            // 处理token解析异常
            return null;
        }
    }

    /**
     * 从token字符串获取userName
     * @param token token字符串
     * @return 用户名
     */
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
            // 处理token解析异常
            return "";
        }
    }

    /**
     * 判断token是否有效
     * @param token token字符串
     * @return 是否过期
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
            // 没有过期，有效，返回false
            return isExpire;
        } catch (Exception e) {
            // 过期或解析异常，返回true
            return true;
        }
    }

    /**
     * 刷新Token
     * @param token 旧token
     * @return 新token
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
            // 使用claims变量获取用户信息，避免重复解析token
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
            // 处理token解析异常
            return null;
        }
    }

    /**
     * 验证token是否有效
     * @param token token字符串
     * @return 是否有效
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
            // token无效
            return false;
        }
    }

    /**
     * 从token字符串获取过期时间
     * @param token token字符串
     * @return 过期时间戳
     */
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
            // 处理token解析异常
            return null;
        }
    }

    /**
     * 主方法用于测试
     * @param args 参数
     */
    public static void main(String[] args) {
        String token = createToken(1L, 1);
        System.out.println("生成的token: " + token);
        System.out.println("从token获取userId: " + getUserId(token));
        System.out.println("从token获取userType: " + getUserType(token));
        System.out.println("token是否有效: " + !isExpiration(token));
        System.out.println("验证token: " + validateToken(token));
        System.out.println("刷新token: " + refreshToken(token));
    }
}
