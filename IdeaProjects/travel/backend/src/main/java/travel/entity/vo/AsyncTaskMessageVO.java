package travel.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskType;
    private String taskId;
    private Map<String, Object> params;
    private Long timestamp;
}
