package travel.user.dto;

import travel.common.entity.user_community.User;

public record UserSummaryResponse(Integer id, String username, String avatar) {

    public static UserSummaryResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(user.getId(), user.getUsername(), user.getAvatar());
    }
}
