package travel.collection.dto;

import java.time.LocalDateTime;

public record ShareStatisticsResponse(
        Long shareId,
        String shareCode,
        String shareTitle,
        Integer shareCount,
        Integer visitCount,
        LocalDateTime createdAt,
        LocalDateTime expireTime,
        Boolean isActive,
        boolean isExpired) {
}
