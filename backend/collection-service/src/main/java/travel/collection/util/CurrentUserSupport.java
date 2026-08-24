package travel.collection.util;

import lombok.experimental.UtilityClass;
import travel.common.entity.user_community.User;
import travel.common.enums.ErrorCodeEnum;
import travel.common.exception.BusinessException;

@UtilityClass
public class CurrentUserSupport {

    public static User requireUser(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return user;
    }
}
