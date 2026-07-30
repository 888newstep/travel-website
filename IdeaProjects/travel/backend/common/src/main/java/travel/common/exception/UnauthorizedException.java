package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class UnauthorizedException extends BusinessException {
    private static final long serialVersionUID = 1L;

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
