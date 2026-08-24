package travel.common.exception;

import travel.common.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class FileException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public FileException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum);
    }

    public FileException(int code, String message) {
        super(code, message);
    }

    public FileException(String message) {
        super(400, message);
    }
}
