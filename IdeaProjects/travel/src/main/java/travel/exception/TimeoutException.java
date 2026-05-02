package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class TimeoutException extends BusinessException {

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
