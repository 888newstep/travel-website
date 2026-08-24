package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class DatabaseException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public DatabaseException(int code, String message) {
        super(code, message);
    }

    public DatabaseException(String message) {
        super(500, message);
    }
}
