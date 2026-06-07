# ${projectName!}-${moduleName!}

公共 API 模块，主要放置 DTO、Entity、枚举、接口定义和跨模块复用的模型类。

## 平台

- Java: ${javaVersion!}
- Bytecode release: ${bytecodeRelease!javaVersion!}
- Namespace: ${namespace!'javax'}

## 构建

```bash
mvn clean package
```

该模块通常不独立启动，业务入口在应用模块。
