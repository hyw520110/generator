# 通用工程生成约束

> 本文件由 generator 的 `ARCHITECTURE_CONVENTIONS` Feature 生成，用于提醒研发在生成后对照工程规范做二次收口。

## 工程口径

- 平台档位：`${platformId!}`，Java `${javaVersion!}`，模板族 `${templateFamily!}`，命名空间 `${namespace!}`。
- 根包名：`${rootPackage!}`。
- 当前模块：`${moduleName!}`。
- 多模块建议：领域服务优先使用 `api` + `service`，BFF 聚合层单独在应用服务中落地。

## 必须保留的工程约束

- Dubbo 对外接口命名为 `*Service`，实现类命名为 `*ServiceImpl`。
- 领域服务实现类必须同时具备 `@DubboService` 和 `@Service`。
- Entity、Mapper、Mapper XML 只允许出现在 `*-service` 模块内部，不暴露给 BFF。
- Controller 只做路由、参数校验和上下文读取；跨领域编排必须放在 `Facade*Service`。
- 接口出入参必须使用强类型 DTO / VO，禁止使用 `Map<String, Object>` 或 `JSONObject` 承载业务字段。
- 查询逻辑必须使用 MyBatis XML SQL，不在 Java 代码里使用 `LambdaQueryWrapper` 拼业务查询。
- 业务表默认需要 `tenant_id`、`app_id`、审计字段、状态字段、软删除字段和中文注释。
- Redis Key、业务状态、类型字段和运营规则必须收敛为枚举或配置，不允许散落魔法值。

## 生成后人工检查

| 检查项 | 处理要求 |
|--------|----------|
| 包名与模块名 | 确认生成结果是否符合当前仓库模块边界，不要把应用规则放入 common/core |
| SQL | 确认 DDL/DML 是否写入主仓库固定 SQL 文件，并补齐中文注释、索引和租户/应用隔离 |
| DTO | 确认 DTO 实现 `Serializable`，字段说明完整，状态覆盖异常分支 |
| Dubbo | 确认 `@DubboReference` 使用统一超时、重试和降级常量 |
| BFF | 如生成 Controller，需要改造成薄 Controller + Facade 编排 |
| 测试 | 至少补充编译验证、Mapper XML 查询测试和高风险接口异常分支测试 |
