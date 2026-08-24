package travel.user.dto;

import travel.common.entity.user_community.User;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Integer id,
        String username,
        String email,
        String avatar,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserProfileResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
