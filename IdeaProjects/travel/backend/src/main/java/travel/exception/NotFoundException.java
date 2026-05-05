package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class NotFoundException extends BusinessException {

    public NotFoundException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public NotFoundException(int code, String message) {
        super(code, message);
    }

    public NotFoundException(String message) {
        super(404, message);
    }
}
