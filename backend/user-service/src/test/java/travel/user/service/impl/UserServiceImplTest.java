package travel.user.service.impl;

import org.junit.jupiter.api.Test;
import travel.common.entity.user_community.User;
import travel.common.exception.BusinessException;
import travel.common.utils.CacheUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    void shouldGenerateSixDigitCaptchaAfterAcquiringCooldown() {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        UserServiceImpl service = new UserServiceImpl(cacheUtil);
        AtomicReference<String> storedCaptcha = new AtomicReference<>();
        when(cacheUtil.tryLock(anyString(), anyString(), eq(60_000L))).thenReturn(true);
        when(cacheUtil.get(anyString(), eq(String.class))).thenAnswer(invocation -> storedCaptcha.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            storedCaptcha.set(invocation.getArgument(1));
            return null;
        }).when(cacheUtil).set(anyString(), anyString(), eq(5L), eq(TimeUnit.MINUTES));

        String captcha = service.sendCaptcha("13800138000");

        assertTrue(captcha.matches("\\d{6}"));
        verify(cacheUtil).tryLock(anyString(), anyString(), eq(60_000L));
    }

    @Test
    void shouldRejectCaptchaRequestDuringCooldown() {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        UserServiceImpl service = new UserServiceImpl(cacheUtil);
        when(cacheUtil.tryLock(anyString(), anyString(), eq(60_000L))).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendCaptcha("13800138000"));

        assertEquals(429, exception.getCode());
    }

    @Test
    void shouldAtomicallyConsumeCaptchaBeforeRegistering() {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        UserServiceImpl service = spy(new UserServiceImpl(cacheUtil));
        User user = user();
        doReturn(false).when(service).existsByPhone(user.getPhone());
        doReturn(false).when(service).existsByUsername(user.getUsername());
        doReturn(true).when(service).save(any(User.class));
        when(cacheUtil.consumeIfEquals("captcha:13800138000", "123456")).thenReturn(true);

        service.register(user, "123456", true);

        verify(cacheUtil).consumeIfEquals("captcha:13800138000", "123456");
        verify(cacheUtil, never()).delete(anyString());
    }

    @Test
    void shouldRejectReplayedCaptchaBeforeDatabaseWrite() {
        CacheUtil cacheUtil = mock(CacheUtil.class);
        UserServiceImpl service = spy(new UserServiceImpl(cacheUtil));
        User user = user();
        doReturn(false).when(service).existsByPhone(user.getPhone());
        doReturn(false).when(service).existsByUsername(user.getUsername());
        when(cacheUtil.consumeIfEquals("captcha:13800138000", "123456")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.register(user, "123456", true));

        verify(service, never()).save(any(User.class));
    }

    private User user() {
        User user = new User();
        user.setUsername("traveler");
        user.setPhone("13800138000");
        user.setPassword("password123");
        return user;
    }
}
