package travel.common.exception;

import org.junit.jupiter.api.Test;
import travel.common.enums.ErrorCodeEnum;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionPropagationTest {

    @Test
    void shouldPreserveBusinessExceptionForGlobalStatusMapping() {
        BusinessException exception = new BusinessException(ErrorCodeEnum.ROUTE_NOT_EXIST);

        assertSame(exception, ExceptionPropagation.propagate(exception));
    }

    @Test
    void shouldWrapCheckedExceptionAsUnexpectedSystemFailure() {
        Exception checkedException = new Exception("database password leaked");

        IllegalStateException result = assertThrows(IllegalStateException.class,
                () -> {
                    throw ExceptionPropagation.propagate(checkedException);
                });

        assertSame(checkedException, result.getCause());
    }
}
