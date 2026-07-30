package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class ForbiddenException extends BusinessException {
    private static final long serialVersionUID = 1L;

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
