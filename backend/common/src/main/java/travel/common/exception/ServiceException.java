package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class ServiceException extends BusinessException {
    private static final long serialVersionUID = 1L;

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
