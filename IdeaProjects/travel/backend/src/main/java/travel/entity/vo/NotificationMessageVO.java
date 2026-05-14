package travel.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String type;
    private String title;
    private String content;
    private Map<String, Object> extraData;
    private Long timestamp;
}
