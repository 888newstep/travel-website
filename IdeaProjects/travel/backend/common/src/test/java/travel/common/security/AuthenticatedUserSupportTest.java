package travel.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.exception.BusinessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuthenticatedUserSupportTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveNumericPrincipal() {
        authenticate(42L);

        assertEquals(42L, AuthenticatedUserSupport.requireUserId());
        assertEquals(42, AuthenticatedUserSupport.requireIntegerUserId());
    }

    @Test
    void shouldResolveStringPrincipal() {
        authenticate("42");

        assertEquals(42L, AuthenticatedUserSupport.requireUserId());
    }

    @Test
    void shouldRejectMissingOrInvalidPrincipal() {
        assertThrows(BusinessException.class, AuthenticatedUserSupport::requireUserId);

        authenticate("anonymousUser");
        assertThrows(BusinessException.class, AuthenticatedUserSupport::requireUserId);

        authenticate("not-a-user-id");
        assertThrows(BusinessException.class, AuthenticatedUserSupport::requireUserId);
    }

    @Test
    void shouldRequireAdminAuthority() {
        authenticate(42L);
        BusinessException exception = assertThrows(
                BusinessException.class, AuthenticatedUserSupport::requireAdmin);
        assertEquals(28001, exception.getCode());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        42L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertDoesNotThrow(AuthenticatedUserSupport::requireAdmin);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
