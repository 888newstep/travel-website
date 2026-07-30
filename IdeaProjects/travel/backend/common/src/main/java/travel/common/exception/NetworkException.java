package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class NetworkException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public NetworkException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public NetworkException(int code, String message) {
        super(code, message);
    }

    public NetworkException(String message) {
        super(503, message);
    }
}
