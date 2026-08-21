package travel.common.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;

@UtilityClass
public class AuthenticatedUserSupport {

    public static Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        try {
            long userId;
            if (principal instanceof Number number) {
                userId = number.longValue();
                if (number.doubleValue() != (double) userId) {
                    throw new NumberFormatException("Non-integral user ID");
                }
            } else {
                userId = Long.parseLong(principal.toString());
            }
            if (userId <= 0) {
                throw new NumberFormatException("Non-positive user ID");
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
    }

    public static Integer requireIntegerUserId() {
        try {
            return Math.toIntExact(requireUserId());
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
    }

    public static Long getUserIdOrNull() {
        try {
            return requireUserId();
        } catch (BusinessException e) {
            return null;
        }
    }

    public static Integer getIntegerUserIdOrNull() {
        try {
            return requireIntegerUserId();
        } catch (BusinessException e) {
            return null;
        }
    }

    public static void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!admin) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
    }
}
