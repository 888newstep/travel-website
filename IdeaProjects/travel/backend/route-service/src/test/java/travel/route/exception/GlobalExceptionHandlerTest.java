package travel.route.exception;

import org.junit.jupiter.api.Test;
import travel.common.exception.GlobalExceptionHandler;
import travel.common.exception.BusinessException;
import travel.common.utils.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void testHandleBusinessException() {
        BusinessException ex = new BusinessException(400, "测试业务异常");
        ResponseEntity<? extends Result<?>> response = handler.handleBusinessException(ex);
        Result<?> result = response.getBody();
        
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, result.getCode());
        assertEquals("测试业务异常", result.getMessage());
    }

    @Test
    public void testHandleGenericException() {
        Exception ex = new RuntimeException("测试系统异常");
        ResponseEntity<? extends Result<?>> response = handler.handleException(ex);
        Result<?> result = response.getBody();
        
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, result.getCode());
    }

    @Test
    void shouldMapAuthorizationAndResourceErrorsToHttpStatus() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                handler.handleBusinessException(new BusinessException(28002, "未授权")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleBusinessException(new BusinessException(28001, "无权限")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleBusinessException(new BusinessException(2001, "路线不存在")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleBusinessException(new BusinessException(22014, "餐厅不存在")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleBusinessException(new BusinessException(22015, "城市不存在")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                handler.handleBusinessException(new BusinessException(20001, "实时数据获取失败")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                handler.handleBusinessException(new BusinessException(20007, "历史人流数据尚未接入")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                handler.handleBusinessException(new BusinessException(1015, "验证码通道未启用")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                handler.handleBusinessException(new BusinessException(5006, "系统依赖错误")).getStatusCode());
    }
}
