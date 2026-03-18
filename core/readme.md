# Generator Core

代码生成器核心模块，提供数据库元数据读取、模板渲染、代码生成等核心功能。

## 一、模块概述

`generator-core` 是整个代码生成器的核心引擎，负责：

- 数据库元数据读取与解析
- 模板引擎渲染（支持 Velocity、FreeMarker 多引擎）
- 代码文件生成与输出
- 配置加载与验证
- 共享资源管理

## 二、技术依赖

### 核心依赖

| 依赖 | 用途 |
|------|------|
| SLF4J + Logback | 日志框架 |
| Apache Velocity | 模板引擎（默认） |
| FreeMarker | 模板引擎（可选） |
| SnakeYAML | YAML 配置文件解析 |
| MySQL Connector | 数据库驱动（默认） |
| Druid | 数据库连接池 |
| Commons IO | 文件操作工具 |
| Commons Lang3 | 字符串/数组工具 |
| Lombok | 代码简化 |
| Caffeine | 本地缓存 |

### 测试依赖

| 依赖 | 用途 |
|------|------|
| JUnit | 单元测试 |
| Apache POI | Word 文档生成测试 |
| Apache PDFBox | PDF 文档生成测试 |
| Commons Lang | 兼容旧版 DbToDoc |

## 三、核心组件

### 3.1 生成器入口

- **`Generator`** - 主生成器类，提供 `main()` 方法和 `execute()` 方法
- **`AbstractGenerator`** - 生成器抽象基类，提供数据库元数据读取、目录操作等基础功能

### 3.2 配置类

| 类 | 说明 |
|----|------|
| `GlobalConf` | 全局配置（输出目录、包名、模块配置等） |
| `DataSourceConf` | 数据源配置（URL、用户名、密码等） |
| `TableConfig` | 表配置（包含/排除规则） |
| `TemplateConfig` | 模板配置 |
| `OutputConfig` | 输出配置 |
| `ProjectConfig` | 项目配置 |

### 3.3 数据库元数据

| 类 | 说明 |
|----|------|
| `Table` | 数据库表元数据（表名、注释、字段列表） |
| `TabField` | 表字段元数据（字段名、类型、注释、是否主键） |
| `PrimaryKeyInfo` | 主键信息 |
| `DatabaseMetadataReader` | 数据库元数据读取器 |
| `QuerySQL` | 不同数据库的查询 SQL 封装 |

### 3.4 类型转换

| 类 | 说明 |
|----|------|
| `TypeConvertor` | 类型转换器接口 |
| `TypeConvertStrategyFactory` | 类型转换策略工厂 |
| `MySqlTypeConvert` | MySQL 类型转换 |
| `OracleTypeConvert` | Oracle 类型转换 |
| `PostgreSqlTypeConvert` | PostgreSQL 类型转换 |
| `SqlServerTypeConvert` | SQL Server 类型转换 |

### 3.5 模板引擎

| 类/接口 | 说明 |
|---------|------|
| `TemplateEngine` | 模板引擎接口 |
| `TemplateRenderer` | 模板渲染器（统一入口） |
| `VelocityEngineImpl` | Velocity 引擎实现 |
| `FreeMarkerEngineImpl` | FreeMarker 引擎实现 |
| `TemplateResource` | 模板资源接口 |
| `FileTemplateResource` | 文件模板资源 |
| `JarTemplateResource` | JAR 包内模板资源 |
| `TemplateLoader` | 模板加载器 |
| `MultiTemplateLoader` | 多模板加载器 |
| `PathTemplateResolver` | 路径模板解析器 |

### 3.6 工具类

| 类 | 说明 |
|----|------|
| `FileUtils` | 文件操作工具（列出文件、判断二进制文件等） |
| `StringUtils` | 字符串工具（驼峰转换、下划线处理等） |
| `ConfigValidator` | 配置验证器 |
| `ResourceManager` | 共享资源管理器 |

### 3.7 枚举类

| 枚举 | 说明 |
|------|------|
| `Component` | 组件类型（mybatis、springmvc、vue 等） |
| `ComponentGroup` | 组件分组 |
| `DBType` | 数据库类型 |
| `FieldType` | 字段类型 |
| `Naming` | 命名策略 |
| `ProjectBuilder` | 项目构建工具（maven/gradle） |
| `ExportFormat` | 导出格式 |

### 3.8 异常类

| 异常 | 说明 |
|------|------|
| `GeneratorException` | 生成器通用异常 |
| `ConfigurationException` | 配置异常 |
| `DatabaseException` | 数据库异常 |
| `TemplateRenderException` | 模板渲染异常 |
| `FileOperationException` | 文件操作异常 |

## 四、核心流程

### 4.1 代码生成流程

```
1. 加载配置文件 (generator.yaml)
   ↓
2. 初始化数据源连接
   ↓
3. 读取数据库元数据（表、字段、注释）
   ↓
4. 创建渲染上下文 (RenderContext)
   ↓
5. 加载模板文件（支持 JAR/文件系统）
   ↓
6. 渲染模板（根据组件配置选择模板）
   ↓
7. 写入生成文件
   ↓
8. 复制共享静态资源
   ↓
9. 输出生成报告
```

### 4.2 模板渲染流程

```
模板文件 → TemplateResource → TemplateRenderer → 选择引擎 (Velocity/FreeMarker) → RenderContext → 输出内容
```

## 五、配置说明

### 5.1 配置文件位置

- 默认：`/generator.yaml`（classpath 根目录）
- 可选：`./conf/generator.yaml`（文件系统）

### 5.2 核心配置项

```yaml
# 数据源配置
dataSource:
  url: jdbc:mysql://localhost:3306/test
  username: root
  pwd: 123456
  driverClassName: com.mysql.jdbc.Driver

# 全局配置
global:
  outputDir: /output/test          # 输出目录
  rootPackage: com.test            # 根包名
  author: author_name              # 作者
  fileOverride: true               # 是否覆盖已存在文件
  templateDir: templates           # 模板目录
  modules: [api,app]               # 模块数组
  components: [mybatis,springmvc]  # 组件配置
  
# 表配置
table:
  include: []    # 包含的表（空表示全部）
  exclude: []    # 排除的表
  tablePrefix: [] # 表名前缀（生成时去除）
  naming: TOCAMEL # 命名策略：NOCHANGE/TOCAMEL
```

## 六、使用示例

### 6.1 命令行方式

```java
public static void main(String[] args) {
    Generator generator = new Generator();
    generator.init();  // 加载默认配置文件
    generator.execute();  // 执行代码生成
}
```

### 6.2 编程方式

```java
// 创建生成器
Generator generator = new Generator();

// 配置数据源
DataSourceConf dataSource = new DataSourceConf();
dataSource.setUrl("jdbc:mysql://localhost:3306/test");
dataSource.setUsername("root");
dataSource.setPassword("123456");
generator.setDataSource(dataSource);

// 配置全局参数
GlobalConf global = new GlobalConf();
global.setOutputDir("/output/test");
global.setRootPackage("com.test");
global.setAuthor("developer");
generator.setGlobal(global);

// 执行生成
generator.execute();
```

### 6.3 自定义模板引擎

```java
// 使用 FreeMarker 引擎
TemplateEngine engine = new FreeMarkerEngineImpl();
TemplateRenderer renderer = new TemplateRenderer(engine);
```

## 七、支持的数据库

| 数据库 | 状态 | 类型转换器 |
|--------|------|-----------|
| MySQL | ✅ 已集成 | `MySqlTypeConvert` |
| Oracle | ✅ 已集成 | `OracleTypeConvert` |
| PostgreSQL | ✅ 已集成 | `PostgreSqlTypeConvert` |
| SQL Server | ✅ 已集成 | `SqlServerTypeConvert` |

## 八、模板目录结构

```
templates/
├── base/           # 基础模板（pom.xml、build.gradle 等）
├── common/         # 公共模板（按组件）
├── commons/        # 兼容旧版公共模板
├── modules/        # 模块模板
├── persistence/    # 持久层模板（Entity、Mapper 等）
├── web/            # Web 层模板（Controller、Service 等）
├── microservice/   # 微服务模板（Dubbo、SpringCloud 等）
└── components/     # 组件模板（按组件类型）
```

## 九、扩展点

### 9.1 自定义组件

1. 在 `Component` 枚举中添加新组件
2. 在 `templates` 目录下创建对应组件的模板目录
3. 在配置文件中配置组件

### 9.2 自定义类型转换

实现 `TypeConvertor` 接口，并在 `TypeConvertStrategyFactory` 中注册。

### 9.3 自定义模板引擎

实现 `TemplateEngine` 接口，并通过 `TemplateRenderer` 使用。

## 十、注意事项

1. **编译版本**: 需要 JDK 1.8+
2. **编码**: 统一使用 UTF-8
3. **Lombok**: 需要 IDE 安装 Lombok 插件
4. **路径遍历防护**: 文件写入时有安全校验，防止非法输出路径
5. **SQL 注入防护**: 表名查询时有字符合法性校验

## 十一、测试

运行单元测试：

```bash
# Maven
mvn test

# Gradle
gradle test
```

## 十二、打包

```bash
# Maven 打包
mvn clean package

# 生成的 JAR 位于 target/generator-core-{version}.jar
```

## 十三、版本历史

- **1.0.1-SNAPSHOT**: 当前版本
  - 支持多模板引擎（Velocity/FreeMarker）
  - 支持多数据库类型转换
  - 支持多模块工程生成
  - 支持组件化配置
