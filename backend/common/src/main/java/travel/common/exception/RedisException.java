package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class RedisException extends BusinessException {
    private static final long serialVersionUID = 1L;

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
