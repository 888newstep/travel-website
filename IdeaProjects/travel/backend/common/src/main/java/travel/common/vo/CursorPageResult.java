package travel.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResult<T> {

    private List<T> records;
    private Integer nextCursor;
    private boolean hasMore;

    private BigDecimal lastRating;
    private Integer lastId;

    public static <T> CursorPageResult<T> empty() {
        return new CursorPageResult<>(Collections.emptyList(), null, false, null, null);
    }

    public static <T> CursorPageResult<T> of(List<T> records, boolean hasMore,
                                              BigDecimal lastRating, Integer lastId) {
        return new CursorPageResult<>(records, lastId, hasMore, lastRating, lastId);
    }

    public String encodeCursor() {
        if (lastRating == null || lastId == null) {
            return null;
        }
        return lastRating.toPlainString() + ":" + lastId;
    }

    public static BigDecimal[] decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = cursor.split(":");
        if (parts.length != 2) {
            return null;
        }
        return new BigDecimal[]{new BigDecimal(parts[0]), new BigDecimal(parts[1])};
    }
}

