package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class NetworkException extends BusinessException {

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
