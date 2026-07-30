package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class TimeoutException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public TimeoutException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public TimeoutException(int code, String message) {
        super(code, message);
    }

    public TimeoutException(String message) {
        super(504, message);
    }
}
