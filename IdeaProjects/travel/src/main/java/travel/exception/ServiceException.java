package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class ServiceException extends BusinessException {

    public ServiceException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public ServiceException(int code, String message) {
        super(code, message);
    }

    public ServiceException(String message) {
        super(500, message);
    }
}
