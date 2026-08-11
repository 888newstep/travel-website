package travel.common.entity.messaging;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RabbitMQ 发布状态记录。
 *
 * <p>DISPATCHED 只表示本地 RabbitTemplate 接受了发送调用，
 * CONFIRMED 才表示 broker 返回了 publisher confirm；二者不能混用。</p>
 */
@Data
@TableName("mq_message_status")
public class MqMessageStatusRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private String messageId;

    @TableField("message_type")
    private String messageType;

    @TableField("exchange_name")
    private String exchangeName;

    @TableField("routing_key")
    private String routingKey;

    @TableField("payload_json")
    private String payloadJson;

    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("last_error")
    private String lastError;

    @TableField("next_attempt_time")
    private LocalDateTime nextAttemptTime;

    @TableField("dispatched_at")
    private LocalDateTime dispatchedAt;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
