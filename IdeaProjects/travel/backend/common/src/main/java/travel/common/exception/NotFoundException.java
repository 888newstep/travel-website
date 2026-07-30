package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class NotFoundException extends BusinessException {
    private static final long serialVersionUID = 1L;

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
