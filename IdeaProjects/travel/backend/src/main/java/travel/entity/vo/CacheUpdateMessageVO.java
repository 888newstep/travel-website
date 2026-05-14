package travel.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheUpdateMessageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cacheKey;
    private String operation;
    private Object data;
    private Long expireTime;
}
