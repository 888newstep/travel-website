package travel.common.mapper.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import travel.common.entity.messaging.MqMessageStatusRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MqMessageStatusMapper extends BaseMapper<MqMessageStatusRecord> {

    int markDispatched(@Param("messageId") String messageId);

    int markConfirmed(@Param("messageId") String messageId);

    int markReturned(@Param("messageId") String messageId, @Param("errorMessage") String errorMessage);

    int markFailed(@Param("messageId") String messageId, @Param("errorMessage") String errorMessage);

    List<MqMessageStatusRecord> findCompensationCandidates(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("limit") int limit);

    int claimForCompensation(
            @Param("id") Long id,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxRetryCount") int maxRetryCount);

    int markCompensationDispatched(@Param("id") Long id);

    int markCompensationFailed(
            @Param("id") Long id,
            @Param("errorMessage") String errorMessage,
            @Param("nextAttemptTime") LocalDateTime nextAttemptTime);
}
