#if($projectBuilder == "MAVEN")
# ${projectName} Parent

聚合构建模块，用于统一管理子模块构建顺序、Java 编译级别和公共依赖版本。

## 平台

- Java: ${javaVersion}
- Bytecode release: ${bytecodeRelease}
- Template family: ${templateFamily}
- Namespace: ${namespace}
- Security: ${security}

## 构建

```bash
mvn clean package
```

只构建后端应用模块：

```bash
mvn -pl ../${projectName}-${moduleName} -am clean package
```

## 生成前验证

生成器侧建议先执行 dry-run 预览输出范围，再正式生成：

```bash
java -jar generator-core.jar --dryRun --include backend,frontend
```

多表并行生成时可按机器配置调整并发度：

```bash
java -jar generator-core.jar --parallelTables 4
```
#end
