
package $sentinelPackage;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

@Configuration
public class SentinelConfiguration {
	@Value("#[[${spring.cloud.zookeeper.connect-string:]]#${connect-string}#[[}]]#")
	private String ZK_SERVER_ADDR;
	@Value("${spring.application.name}")
	private String appName;
	@PostConstruct
	public void init() {
		final String groupId = "sentinel_rule_config";
		ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ZookeeperDataSource<>(ZK_SERVER_ADDR,
				groupId, appName, source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
				}));
		FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
	}
//	@Bean
//	public SentinelResourceAspect sentinelResourceAspect() {
//		return new SentinelResourceAspect();
//	}
}
