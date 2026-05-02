package travel.exception;

import travel.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class FileException extends BusinessException {

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
