package ${packagePath};

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * RocketMQ 消费者示例
 */
@Service
@RocketMQMessageListener(topic = "demo-topic", consumerGroup = "${projectName}_producer")
public class RocketMQConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
