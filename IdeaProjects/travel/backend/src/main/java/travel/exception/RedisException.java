package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class RedisException extends BusinessException {

    public RedisException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public RedisException(int code, String message) {
        super(code, message);
    }

    public RedisException(String message) {
        super(500, message);
    }
}
