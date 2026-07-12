# Java 21 Boot 3 Dubbo Generator Preset

本文件记录 generator 侧已经集成的通用 Java 21 + Spring Boot 3 + Dubbo 工程预设，不绑定具体项目品牌、应用名或仓库名。

## 已集成

| 能力 | 落地点 | 用法 |
|------|--------|------|
| 版本档 | `core/src/main/resources/compatibility.yml` | `global.platformId: java21-boot3-dubbo` |
| 配置样例 | `core/src/main/resources/presets/java21-boot3-dubbo.yaml` | 复制为实际运行配置后调整数据源、输出目录和表范围 |
| 工程约束 Feature | `Feature.ARCHITECTURE_CONVENTIONS` | 在 `global.features` 中加入 `ARCHITECTURE_CONVENTIONS` |
| 生成后检查清单 | `templates/freemarker/modules/{1}/#architecture_conventions#/ARCHITECTURE_GENERATOR_CONVENTIONS.md.ftl` | 生成服务模块时输出工程约束清单 |
| Dubbo 引用默认值 | `templates/freemarker/modules/{1}/[sourceDirectory]/#architecture_conventions#/rpc/RpcReferenceDefaults.java.ftl` | 生成统一超时、重试、降级常量，避免在业务类里散落魔法值 |
| 通用运行配置 | `templates/freemarker/modules/{1}/[resourceDirectory]/#architecture_conventions#/application-common.yml.ftl` | 生成虚拟线程、MyBatis XML、Dubbo 消费端默认值、上下文头校验等基础配置 |
| 通用运维脚本资产 | `templates/assets/*.sh` | 可按需复制到生成工程，覆盖打包、启停、部署、状态查看和 Git 自动化 |
| DevOps 基础模板 | `templates/freemarker/modules/parent/#devops#/` | 提供 Docker、docker-compose、GitLab CI、K8s 的基础骨架 |

## 当前适合生成的内容

- Java 21 / Spring Boot 3 / Dubbo 3 / MyBatis-Plus 版本口径。
- 统一响应、分页结果、基础实体、全局异常、MyBatis-Plus 配置和 Mapper XML 基础结构。
- 领域服务 `api` + `service` 骨架、Dubbo Provider/Consumer 命名约束和引用默认值。
- Nacos、Redis、RocketMQ、Sentinel、OpenAPI、JWT、审计、幂等、租户等基础组件组合。
- 通用打包、启停、部署、状态查看、容器化和 CI/CD 骨架。
- 生成后工程规范检查清单。

## 暂不自动生成的内容

- 训练结算、奖励发放、风控判定、账本冲正、智能助手回答等完整业务规则。
- 具体应用的完整 UI 页面。
- 生产审批流、风控模型和资金/权益账本的最终业务实现。

这些内容必须结合产品、运营、合规和客户现场规则评审后落地，generator 只提供骨架、状态、接口和检查清单。

## 配置文件职责

- `generator.yaml`：生成器默认运行配置和完整配置字段示例，保留且只维护一份。
- `compatibility.yml`：Java、Spring Boot 及相关组件版本的统一兼容矩阵，命令行、Web 和模板共用，不能由 preset 替代。
- `presets/*.yaml`：经过约束的可选组合样例，不会自动覆盖 `generator.yaml`，使用时需显式选择或复制后调整。

## Java 21 + Spring Boot 4

`presets/java21-boot4.yaml` 提供显式启用的实验档，版本口径由 `compatibility.yml` 中的 `java21-boot4` 维护。该档不设为 Java 21 默认项，当前只承诺 preset 中列出的基础组件组合；Dubbo、Nacos、Sentinel、RocketMQ、Knife4j 等组件应在完成独立兼容验证后再加入 Boot 4 preset。

Boot 4 模板判断统一使用兼容矩阵派生的 `springBootMajor`，不再根据 `platformId` 是否包含 `boot3` 推断 API 写法。
