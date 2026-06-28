package ${packagePath};

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者示例
 */
@Component
public class KafkaConsumer {

    @KafkaListener(topics = "demo-topic", groupId = "${projectName}_group")
    public void listen(String message) {
        System.out.println("Received message in group: " + message);
    }
}
