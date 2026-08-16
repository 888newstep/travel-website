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
        String[] parts = cursor.split(":", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            BigDecimal rating = new BigDecimal(parts[0]);
            BigDecimal id = new BigDecimal(parts[1]);
            if (rating.signum() < 0
                    || id.signum() <= 0
                    || id.scale() > 0
                    || id.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                return null;
            }
            return new BigDecimal[]{rating, id};
        } catch (NumberFormatException ex) {
            // 游标来自客户端，非法格式应按“无效游标”处理，不能冒泡为 500。
            return null;
        }
    }
}
