package travel.route.dto.route;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties({
        "id", "userId", "viewCount", "likeCount", "isPublic",
        "status", "version", "createdAt", "updatedAt"
})
public class CreateRouteRequest {

    @NotBlank(message = "路线标题不能为空")
    @Size(max = 100, message = "路线标题不能超过100个字符")
    private String title;

    @Size(max = 2000, message = "路线描述不能超过2000个字符")
    private String description;

    @NotNull(message = "城市ID不能为空")
    @Positive(message = "城市ID必须为正整数")
    private Integer cityId;

    @NotNull(message = "路线天数不能为空")
    @Min(value = 1, message = "路线天数不能少于1天")
    @Max(value = 30, message = "路线天数不能超过30天")
    private Integer durationDays;

    @Size(max = 20, message = "路线难度不能超过20个字符")
    private String difficulty;

    @Size(max = 200, message = "封面图地址不能超过200个字符")
    private String coverImage;
}
