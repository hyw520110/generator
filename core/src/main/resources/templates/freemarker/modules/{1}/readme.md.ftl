# ${projectName!}-${moduleName!}

后端应用模块，包含 Spring Boot 启动类、Controller、Service、Mapper、资源配置和可选组件集成。

## 平台

- Java: ${javaVersion!}
- Bytecode release: ${bytecodeRelease!javaVersion!}
- Spring Boot: ${springboot_version!}
- JSON mapper: ${json_type!'jackson'}
- Namespace: ${namespace!'javax'}

## 本地启动

IDE 中直接运行 `Booter`，或执行：

```bash
mvn clean package
```

打包完成后解压 `target` 目录中的应用包，进入 `bin` 目录运行：

```bash
sh startup.sh
```

## 配置

数据库连接在 `src/main/resources/application.properties` 或 `application.yml` 中维护。生产环境建议通过环境变量或外部配置覆盖账号密码。

<#if ZOOKEEPER?? && ZOOKEEPER>
## Zookeeper 配置

如启用 Zookeeper，可用 `zookeeper.data` 初始化分布式配置：

```bash
zkCli.sh < zookeeper.data
```

也可以运行 `ZkTool` 从配置文件导入初始数据。
</#if>

<#if JWT?? && JWT>
## JWT

默认 token 请求头为 `X-USER-TOKEN`。上线前应替换密钥、收紧白名单，并只开放真实匿名接口。
</#if>

## 验证

```bash
mvn test
mvn clean package
```
