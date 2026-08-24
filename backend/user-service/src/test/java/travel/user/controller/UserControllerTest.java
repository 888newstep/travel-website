package travel.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import travel.common.entity.user_community.User;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;
import travel.common.service.MessageProducerService;
import travel.common.utils.Result;
import travel.user.dto.UserProfileResponse;
import travel.user.dto.UserSummaryResponse;
import travel.user.service.UserService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private MessageProducerService messageProducerService;

    @InjectMocks
    private UserController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotExposePasswordInCurrentUserResponse() throws Exception {
        User user = user();
        when(userService.getCurrentUser()).thenReturn(user);

        Result<UserProfileResponse> result = controller.getCurrentUser();
        String json = objectMapper.writeValueAsString(result);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("encoded-password"));
        assertTrue(json.contains("13800138000"));
    }

    @Test
    void shouldReturnOnlyPublicFieldsForUserSummary() throws Exception {
        User user = user();
        when(userService.getById(42)).thenReturn(user);

        Result<UserSummaryResponse> result = controller.getUser(42);
        String json = objectMapper.writeValueAsString(result);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("13800138000"));
        assertNull(result.getData().avatar());
    }

    @Test
    void shouldUpdateOnlyAuthenticatedUser() {
        authenticate(42L);
        User request = new User();
        request.setId(99);
        request.setUsername("updated");
        when(userService.updateProfile(request)).thenReturn(request);

        Result<Boolean> result = controller.updateUser(request);

        assertTrue(result.getData());
        verify(userService).updateProfile(argThat(user -> user.getId().equals(42)));
    }

    @Test
    void shouldIgnoreDeletePathUserId() {
        authenticate(42L);
        when(userService.removeById(42)).thenReturn(true);

        Result<Boolean> result = controller.deleteUser(99);

        assertTrue(result.getData());
        verify(userService).removeById(42);
    }

    @Test
    void shouldRejectCaptchaWhenDemoModeIsDisabled() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.sendCaptcha("13800138000"));

        assertEquals(ErrorCodeEnum.CAPTCHA_CHANNEL_UNAVAILABLE.getCode(), exception.getCode());
        verify(userService, org.mockito.Mockito.never()).sendCaptcha(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldExposeGeneratedCaptchaOnlyInDemoMode() {
        ReflectionTestUtils.setField(controller, "captchaDemoMode", true);
        when(userService.sendCaptcha("13800138000")).thenReturn("123456");

        Result<UserController.CaptchaResponse> result = controller.sendCaptcha("13800138000");

        assertEquals("123456", result.getData().getDemoCode());
        verify(userService).sendCaptcha("13800138000");
    }

    @Test
    void shouldKeepLoginSuccessfulWhenNotificationPublishingFails() {
        User user = user();
        UserController.LoginRequest request = new UserController.LoginRequest();
        request.setUsername("traveler");
        request.setPassword("password123");
        when(userService.login("traveler", "password123")).thenReturn("jwt-token");
        when(userService.getOne(any())).thenReturn(user);
        doThrow(new IllegalStateException("rabbit unavailable"))
                .when(messageProducerService)
                .sendNotification(any(), any(), any(), any());

        Result<java.util.Map<String, String>> result = controller.login(request);

        assertEquals("jwt-token", result.getData().get("token"));
    }

    @Test
    void shouldNotExposeCredentialsInRequestToString() {
        UserController.RegisterRequest registerRequest = new UserController.RegisterRequest();
        registerRequest.setUsername("traveler");
        registerRequest.setPassword("register-password");
        registerRequest.setCaptcha("123456");

        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setUsername("traveler");
        loginRequest.setPassword("login-password");

        UserController.ChangePasswordRequest changeRequest = new UserController.ChangePasswordRequest();
        changeRequest.setOldPassword("old-password");
        changeRequest.setNewPassword("new-password");

        UserController.ResetPasswordRequest resetRequest = new UserController.ResetPasswordRequest();
        resetRequest.setCaptcha("654321");
        resetRequest.setNewPassword("reset-password");

        UserController.RefreshTokenRequest refreshRequest = new UserController.RefreshTokenRequest();
        refreshRequest.setOldToken("refresh-token");

        String logRepresentation = String.join(
                "|",
                registerRequest.toString(),
                loginRequest.toString(),
                changeRequest.toString(),
                resetRequest.toString(),
                refreshRequest.toString());

        assertTrue(logRepresentation.contains("traveler"));
        assertFalse(logRepresentation.contains("register-password"));
        assertFalse(logRepresentation.contains("123456"));
        assertFalse(logRepresentation.contains("login-password"));
        assertFalse(logRepresentation.contains("old-password"));
        assertFalse(logRepresentation.contains("new-password"));
        assertFalse(logRepresentation.contains("654321"));
        assertFalse(logRepresentation.contains("reset-password"));
        assertFalse(logRepresentation.contains("refresh-token"));
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private User user() {
        User user = new User();
        user.setId(42);
        user.setUsername("traveler");
        user.setPhone("13800138000");
        user.setPassword("encoded-password");
        return user;
    }
}
