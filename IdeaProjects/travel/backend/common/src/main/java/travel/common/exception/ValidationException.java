package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class ValidationException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public ValidationException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public ValidationException(int code, String message) {
        super(code, message);
    }
}
