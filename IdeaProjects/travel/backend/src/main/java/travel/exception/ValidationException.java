package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class ValidationException extends BusinessException {

    public ValidationException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public ValidationException(int code, String message) {
        super(code, message);
    }
}
