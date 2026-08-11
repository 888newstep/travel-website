package travel.route.exception;

import org.junit.jupiter.api.Test;
import travel.common.exception.GlobalExceptionHandler;
import travel.common.exception.BusinessException;
import travel.common.utils.Result;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void testHandleBusinessException() {
        BusinessException ex = new BusinessException(400, "测试业务异常");
        Result<?> result = handler.handleBusinessException(ex);
        
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("测试业务异常", result.getMessage());
    }

    @Test
    public void testHandleGenericException() {
        Exception ex = new RuntimeException("测试系统异常");
        Result<?> result = handler.handleException(ex);
        
        assertNotNull(result);
        assertEquals(500, result.getCode());
    }
}
