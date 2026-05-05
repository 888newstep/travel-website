package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public ForbiddenException(int code, String message) {
        super(code, message);
    }

    public ForbiddenException(String message) {
        super(403, message);
    }
}
