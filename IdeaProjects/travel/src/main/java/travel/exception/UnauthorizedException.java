package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public UnauthorizedException(int code, String message) {
        super(code, message);
    }

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
