package travel.common.exception;

import lombok.experimental.UtilityClass;

/**
 * 将控制器中意外捕获的异常重新交给全局异常处理器。
 */
@UtilityClass
public class ExceptionPropagation {

    public static RuntimeException propagate(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Unexpected checked exception", exception);
    }
}
