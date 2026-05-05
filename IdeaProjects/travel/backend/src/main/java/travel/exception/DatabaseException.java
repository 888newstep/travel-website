package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class DatabaseException extends BusinessException {

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
