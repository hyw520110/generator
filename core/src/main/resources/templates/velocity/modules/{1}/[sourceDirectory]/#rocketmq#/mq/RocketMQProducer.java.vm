package ${packagePath};

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 生产者封装
 */
@Component
public class RocketMQProducer {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送普通消息
     * @param topic 主题
     * @param msg 消息体
     */
    public void sendMessage(String topic, Object msg) {
        if (rocketMQTemplate != null) {
            rocketMQTemplate.convertAndSend(topic, msg);
        }
    }
}
