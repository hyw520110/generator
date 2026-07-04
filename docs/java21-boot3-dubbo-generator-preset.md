# Java 21 Boot 3 Dubbo Generator Preset

本文件记录 generator 侧已经集成的通用 Java 21 + Spring Boot 3 + Dubbo 工程预设，不绑定具体项目品牌、应用名或仓库名。

## 已集成

| 能力 | 落地点 | 用法 |
|------|--------|------|
| 版本档 | `core/src/main/resources/compatibility.yml` | `global.platformId: java21-boot3-dubbo` |
| 配置样例 | `core/src/main/resources/generator-java21-boot3-dubbo.yaml` | 复制为实际运行配置后调整数据源、输出目录和表范围 |
| 工程约束 Feature | `Feature.ARCHITECTURE_CONVENTIONS` | 在 `global.features` 中加入 `ARCHITECTURE_CONVENTIONS` |
| 生成后检查清单 | `templates/freemarker/modules/{1}/#architecture_conventions#/ARCHITECTURE_GENERATOR_CONVENTIONS.md.ftl` | 生成服务模块时输出工程约束清单 |

## 当前适合生成的内容

- Java 21 / Spring Boot 3 / Dubbo 3 / MyBatis-Plus 版本口径。
- 领域服务基础骨架和 Mapper XML 基础结构。
- Nacos、Redis、RocketMQ、Sentinel、OpenAPI 等基础组件组合。
- 生成后工程规范检查清单。

## 暂不自动生成的内容

- 训练结算、奖励发放、风控判定、账本冲正、投资助手回答等完整业务规则。
- SenseAI / VPropTrader 的完整 UI 页面。
- 生产审批流、风控模型和资金/权益账本的最终业务实现。

这些内容必须结合产品、运营、合规和客户现场规则评审后落地，generator 只提供骨架、状态、接口和检查清单。
