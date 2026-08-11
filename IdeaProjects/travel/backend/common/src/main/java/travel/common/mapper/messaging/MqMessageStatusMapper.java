package travel.common.mapper.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import travel.common.entity.messaging.MqMessageStatusRecord;

@Mapper
public interface MqMessageStatusMapper extends BaseMapper<MqMessageStatusRecord> {

    int markDispatched(@Param("messageId") String messageId);

    int markConfirmed(@Param("messageId") String messageId);

    int markReturned(@Param("messageId") String messageId, @Param("errorMessage") String errorMessage);

    int markFailed(@Param("messageId") String messageId, @Param("errorMessage") String errorMessage);
}
