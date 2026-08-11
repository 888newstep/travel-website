package travel.route.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * 路线规划中的固定时间窗口，时间格式为 HH:mm。
 */
public class AITimeWindow {

    private static final String TIME_PATTERN = "([01]\\d|2[0-3]):[0-5]\\d";

    @NotBlank(message = "时间窗口开始时间不能为空")
    @Pattern(regexp = TIME_PATTERN, message = "开始时间必须为 HH:mm 格式")
    private String start;

    @NotBlank(message = "时间窗口结束时间不能为空")
    @Pattern(regexp = TIME_PATTERN, message = "结束时间必须为 HH:mm 格式")
    private String end;

    public AITimeWindow() {
    }

    public AITimeWindow(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @JsonIgnore
    @AssertTrue(message = "时间窗口结束时间必须晚于开始时间")
    public boolean isChronological() {
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            return true;
        }
        try {
            return !LocalTime.parse(end).isBefore(LocalTime.parse(start));
        } catch (DateTimeParseException e) {
            // 具体格式错误交给 @Pattern 返回，避免覆盖更准确的校验消息。
            return true;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String start;
        private String end;

        public Builder start(String start) {
            this.start = start;
            return this;
        }

        public Builder end(String end) {
            this.end = end;
            return this;
        }

        public AITimeWindow build() {
            return new AITimeWindow(start, end);
        }
    }
}
