## 一、简介

**代码生成器/脚手架**，可全自动生成前后台工程和代码，提高开发效率。

只需配置数据源和生成参数，即可一键生成完整的前后端项目代码，包含：
- **后端工程**：Spring Boot/Spring Cloud 微服务架构
- **前端工程**：Vue + Ant Design Pro 或 Thymeleaf 模板
- **完整功能**：用户认证、权限管理、CRUD 接口、页面、API 文档等

### 主要功能：

- **数据库支持**：主流关系型数据库 (MySQL、Oracle、PostgreSQL、SQLServer)
- **表生成策略**：整库生成、指定部分表生成、排除指定表生成 (支持复合主键)
- **构建工具**：支持 Gradle、Maven 或不生成构建脚本
- **多模块工程**：支持单工程和多模块工程，模块名可自定义配置
- **模板自定义**：排版风格、Java 类注释支持自定义 (修改 `comment.vm` 模板文件)
- **组件扩展**：组件特性自定义及扩展
- **双模板引擎**：FreeMarker / Velocity，可根据项目技术栈选择
- **表关系图**：可视化展示数据库表之间的外键关系，支持实线/虚线区分必选/可选关系
- **数据库文档导出**：支持导出 Word (.docx) 和 PDF 格式的数据库表结构文档

### 模板引擎说明：

本项目支持两种模板引擎，模板文件命名规范：

- **FreeMarker 模板**：文件扩展名为 `.ftl`，语法示例：`<#if condition>...</#if>`
- **Velocity 模板**：文件扩展名为 `.vm`，语法示例：`#if(condition)...#end`

在 `generator.yaml` 中通过 `templateEngine` 配置项切换引擎：

```yaml
global:
  # 模板引擎类型，可选值：freemarker, velocity
  templateEngine: freemarker
```

**注意**：模板文件必须使用正确的扩展名，否则会被当作普通文件直接复制，导致条件判断等模板语法不生效。

### 技术组件：

- **前端页面**：ant design pro vue、thymeleaf
- **用户认证鉴权**：jwt、shiro
- **流量哨兵 (流量控制)**：sentinel
- **分布式配置&注册中心&接口限流规则**：zookeeper
- **分布式服务治理**：dubbo
- **微服务**：springboot、springcloud alibaba
- **分布式消息中间件**：rocketmq
- **分布式缓存**：redis 集群&切片缓存
- **APM 系统 (分布式实时数据链路追踪)**：skywalking、zipkin
- **持久层**：mybatis/mybatis-plus、JPA

### 生成器版本

- **命令行版**：交互式命令行，使用简单
- **JAVA 版**：IDE 中运行，适合集成到现有项目
- **Web 版**：可视化界面，操作直观

选择其中一个版本执行即可，推荐**命令行版**(使用简单)

### Web 版操作说明

1. **启动服务**后访问：`http://localhost:8000/generator/code`

2. **三步完成代码生成**：
   - **步骤 1：全局设置** - 配置生成目录、包名、工程模块等（带 * 为必填项）
   - **步骤 2：组件选择** - 选择视图类型、构建工具、框架版本、中间件等
   - **步骤 3：数据源&代码生成** - 配置数据库连接，查询表，选择表后点击生成

3. **生成文档**：在步骤 3 中，点击"生成文档"下拉菜单可导出：
   - **生成 Word 文档**：导出 `.docx` 格式的数据库表结构文档
   - **生成 PDF 文档**：导出 `.pdf` 格式的数据库表结构文档

4. **表关系图**：在步骤 3 中，点击"关系图"下拉菜单可查看：
   - **选中表关系图**：显示已选中表之间的关系
   - **所有表关系图**：显示整个数据库的表关系

5. **错误提示**：生成失败时，错误消息显示在生成按钮同一行

### 预览图

**cmd 版：**
![cmd 版](images/cmd.jpg)

**web 版：**
![web 版](images/web1.jpg)
![web 版](images/web2.jpg)
![web 版](images/web3.jpg)

**生成的前后端工程：**
![生成工程](images/projects.jpeg)

**Vue(ant design pro vue) 登录页面：**
![登录](images/login.png)

**主页面菜单：**
![主页面](images/main.png)

**数据库监控：**
![druid](images/db.png)

**Swagger 定制化接口文档：**
![swagger](images/api.png)

---

## 二、快速开始

### 环境要求

- **JDK 版本**：JDK 17 或更高版本
- **构建工具**：Maven 3.6+ 或 Gradle 7.0+
- **数据库**：MySQL 5.7+ / Oracle 11g+ / PostgreSQL 9.6+ / SQL Server 2016+
- **Node.js**：14.0+（生成 Vue 前端工程时需要）

---

### 1、命令行版生成器

1. **获取程序**：通过 [release](https://github.com/hyw520110/generator/releases/) 下载最新 zip 包，或通过源码构建：
   ```bash
   # 工程根目录下执行
   mvn clean package
   ```
   在 cmd 工程 target 目录下获取 zip 包

2. **解压**：解压 zip 包到任意目录

3. **运行**：执行 `bin` 目录下的命令脚本：
   - Windows: `run.bat`
   - Linux/Mac: `run.sh`

4. **配置**：根据命令行提示，输入数据源配置和生成配置（生成目录、根包等）
   - 一般只需输入数据源配置，其他配置直接回车保持默认即可

---

### 2、Web 版生成器

1. **构建并启动：**
   ```bash
   # 进入项目根目录
   cd generator
   
   # 方式一：使用 Docker（推荐）
   docker-compose up -d
   
   # 方式二：直接运行
   ./package.sh  # 打包
   java -jar cmd/target/generator-*.jar  # 启动
   ```

2. **访问页面：**
   ```
   http://localhost:8000/generator/code
   ```

3. **操作步骤：**
   - **步骤 1：全局设置** - 配置生成目录、包名、工程模块等（带 `*` 为必填项）
   - **步骤 2：组件选择** - 选择视图类型、构建工具、框架版本、中间件等
   - **步骤 3：数据源&代码生成** - 配置数据库连接，选择表，点击生成

---

### 3、JAVA 版生成器

#### 3.1 生成器配置 (generator.yaml)：

```yaml
# 以下必须修改
url: jdbc:mysql://localhost:3306/test?useUnicode=true&autoReconnect=true&characterEncoding=UTF-8
username: root
pwd: 123456

outputDir: /output/test
rootPackage: com.test
```

以上为必须修改的配置项，其他均为可选修改项，更多可选配置项说明见配置文件注释

#### 3.2 生成代码：

1. 导入源码到 IDE(安装配置好 maven/gradle)
2. 修改生成器配置文件 `generator.yaml`
3. 执行 `Generator.java` 的 `main` 方法

---

## 三、应用服务启动

代码生成完成后，资源管理器会自动打开生成路径，默认包含后台工程和前台工程 (vue)

### 1、后台工程初始化

**如配置的组件中不包含 zookeeper**，则默认是本地配置文件 (`application.yml`)，根据需要修改配置或完全保持默认配置

**如配置的组件中包含 zookeeper**，则推荐使用 zookeeper 分布式配置（非必须），初始化 zookeeper 配置数据：

1. 进入生成的源码根目录下的 `app` 目录
2. 打开 `src/test/resources/zookeeper.data` 全选复制
3. cmd 进入 zookeeper 的 bin 目录下执行：
   ```bash
   zkCli -server localhost:2181
   # 粘贴内容后回车
   ```

### 2、启动后台服务

**方式一：IDE 启动**
- 把生成的源码导入 IDE(maven/gradle 工程)
- 执行 `Boot.java` 启动服务
- 服务启动完成后自动打开浏览器 (profile 为 dev 时)

**方式二：命令行启动**
1. 进入 `parent` 目录，执行 `mvn package`
2. 进入 `app` 目录下的 `target` 目录，解压 zip 包到任意目录
3. 进入解压目录，如需修改配置，则先修改配置
4. 进入 `bin` 目录，执行启动脚本：
   ```bash
   # Windows
   start.bat
   
   # Linux/Mac
   ./start.sh
   ```
   服务启动完成后自动打开浏览器 (profile 为 dev 时)

### 3、启动前端工程

如组件包含 vue(默认配置) 时，会自动生成 vue 工程

本地环境需要先安装 [node](https://nodejs.org/en/)，然后命令行进入生成目录下的 `vue` 目录，执行：

```bash
yarn install
yarn run serve
```

---

## 四、自定义特性

### 1. 自定义工程构建工具
- maven (pom.xml)
- gradle (build.gradle)
- 无

### 2. 自定义工程模块
- 支持生成单工程和多模块工程
- 多模块工程的模块名可自定义

### 3. 父类自定义
- dao、service、controller 层的父类自定义
- 在配置文件中指定即可

### 4. 自定义文件名
- entity、dao、service、controller 层的类名及配置文件名支持自定义生成

### 5. 自定义模板

1. 复制 `templates` 目录重命名，修改模板文件
2. 在配置文件中修改模板目录名

### 6. 扩展组件

1. 在配置文件中指定模块名数组 (`modules`),如模块配置为 `[api,app]`,已有的组件配置为 `[mybatis,springmvc]`,新增 dubbo 配置:`[mybatis,springmvc,dubbo]`
2. 在模板目录 (`templates`) 下新建目录，目录名为 `{模块数组 index}`,如 dubbo 的配置文件在 app 模块中，则新建 `{2}` 文件夹，在文件夹下新建（文件夹）模板文件
3. 在配置文件中新增组件配置 (非必须),配置模板所需的配置信息

### 7. 更多自定义特性
查看生成器配置文件或源码...

---

## 五、数据库文档导出

### 功能说明

生成器支持将数据库表结构导出为文档，方便团队协作和数据库文档管理：

- **Word 文档 (.docx)**：使用 Apache POI 生成，兼容 Microsoft Word、WPS 等
- **PDF 文档 (.pdf)**：使用 Apache PDFBox 生成，支持中文字体

### 文档内容

导出的文档包含以下信息：
- 数据库名称、生成时间
- 表列表概览（表名、注释、记录数）
- 每张表的详细字段信息（字段名、类型、长度、是否为空、默认值、注释、是否主键）

### PDF 中文字体配置

PDF 导出需要配置中文字体，否则中文会显示为乱码。在 `application.yml` 中配置：

```yaml
app:
  pdf:
    fonts:
      # macOS 字体配置（按优先级排序）
      macos: /System/Library/Fonts/Supplemental:Arial Unicode.ttf, /System/Library/Fonts:PingFang*.ttc
      # Windows 字体配置（按优先级排序）
      windows: C:/Windows/Fonts:simhei.ttf, C:/Windows/Fonts:simsun.ttc, C:/Windows/Fonts:msyh.ttc
      # Linux 字体配置（按优先级排序）
      linux: /usr/share/fonts/truetype/droid:DroidSansFallback*.ttf, /usr/share/fonts/truetype/wqy:wqy-microhei*.ttc
```

**配置格式**：`目录路径:文件名模式`，支持 `*` 通配符，多项用逗号分隔。系统按配置顺序依次查找，返回第一个匹配的字体文件。

---

# 六、TODO LIST

1. 各主流关系型数据库驱动包集成及验证（默认只集成 mysql 驱动包，其他数据库集成相应的数据库驱动包即可）
2. 增加分布式消息中间件 rocketmq、kafka
3. redis 切片缓存 key 优化：示例设置过于简单，需考虑唯一性以及序列化与反序列化

---

# 七、FAQ

### 1. GitHub 图片不显示

**解决方案**：修改 hosts 文件，添加以下内容：

```
199.232.28.133 raw.githubusercontent.com
199.232.68.133 gist.githubusercontent.com
199.232.28.133 cloud.githubusercontent.com
199.232.28.133 camo.githubusercontent.com
199.232.28.133 avatars0.githubusercontent.com
199.232.68.133 avatars1.githubusercontent.com
199.232.28.133 avatars2.githubusercontent.com
199.232.68.133 avatars3.githubusercontent.com
199.232.68.133 avatars4.githubusercontent.com
199.232.68.133 avatars5.githubusercontent.com
199.232.68.133 avatars6.githubusercontent.com
199.232.68.133 avatars7.githubusercontent.com
199.232.68.133 avatars8.githubusercontent.com
```

### 2. 启动服务报错：ClassCastException

**错误信息：**
```
java.lang.UnsupportedClassVersionError cannot be cast to [Ljava.lang.Object;
```

**原因：**
指定生成 jdk1.8 配置，而运行环境的 jdk 版本低于 1.8

**解决方案：**
- 安装 jdk1.8/jre1.8，并设置环境变量
- 或者在 pom 中指定 jdk 为 1.7，并去掉 zipkin 数据追踪依赖、mq 消息中间件依赖

### 3. PDF 导出中文显示乱码

**原因**：系统未找到可用的中文字体

**解决方案**：
1. 检查 `application.yml` 中的 `app.pdf.fonts` 配置
2. 确保配置的字体路径和文件名正确
3. 可以安装额外字体或将字体文件复制到配置的目录

---

## 八、架构设计与优化建议

为了持续提升生成器的专业性、健壮性与执行效率，本项目在架构层面遵循以下优化准则：

> **实现进度：8/12 (67%)**

### ✅ 已实现

#### 1. 模板引擎抽象化与按需加载 ✅
- **细节**：通过定义 `TemplateEngine` 接口解耦具体的渲染引擎，并引入延迟初始化策略。
- **逻辑**：只有在渲染过程中真正需要特定引擎（Velocity/FreeMarker）时才通过工厂创建实例，显著降低了系统的启动内存消耗。
- **实现位置**：`TemplateEngineFactory` + `TemplateRenderer`

#### 5. 统一路径解析协议 (PathResolver) ✅
- **细节**：废弃了 `Generator.java` 中硬编码的路径解析逻辑，统一采用 `PathTemplateResolver` 接口及其实现。
- **逻辑**：全面支持 `${table.beanName}` 等语义化占位符，使输出路径的配置与模板解析逻辑彻底解耦，极大提升了代码的维护性。
- **实现位置**：`DefaultPathTemplateResolver.java` + `Generator.java`

#### 6. 智能资源识别与二进制安全 ✅
- **细节**：在 `FileUtils` 中集成了基于魔数（Magic Number）的二进制判定逻辑。
- **逻辑**：系统能自动识别非文本文件（如图片、静态库等）并执行二进制流式拷贝，规避了非文本文件进入渲染引擎导致的乱码或损坏风险。
- **实现位置**：`FileUtils.java` + `Generator.java`

#### 12. 静态资源共享与多态分发 ✅
- **细节**：在 `templates/assets` 下建立了统一的静态资产库，彻底从 `freemarker` 和 `velocity` 目录中剥离了图片、脚本等二进制文件。
- **逻辑**：引入了 **“虚拟资源映射”** 机制：
    *   **脚本类 (assets/scripts/)**：自动解析映射到目标工程的 **模块根目录**。
    *   **根资源 (assets/root/)**：映射到模块根目录。
    *   **组件资源 (assets/commons/...)**：自动映射到 **src/main/resources/static** 等资源目录。
- **价值**：实现了资源的一处存放、两处共用。避免了二进制资源因误入渲染引擎导致的损坏，且极大简化了脚本文件的复用逻辑。
- **实现位置**：`Generator.java` + `DefaultPathTemplateResolver.java`

#### 4. 元数据缓存策略 ✅
- **细节**：集成 Caffeine 缓存并提供可配置的开关机制。
- **逻辑**：通过 `global.enableCache` 配置，平衡"频繁修改表结构"与"快速重复生成"的性能需求。
- **实现位置**：`AbstractGenerator.java`

#### 7. 模板预编译缓存 ✅
- **细节**：在执行生成前，预先解析 `components` 下的常驻模板并缓存在内存中。
- **逻辑**：消除循环渲染各表时的重复磁盘 IO 与语法解析开销，显著提升吞吐量。
- **实现位置**：`Generator.java`

#### 9. 依赖版本中心化管理 ✅
- **细节**：在项目根目录 `pom.xml` 中集中定义版本及依赖管理。
- **逻辑**：确保生成的所有子模块引用的版本严格一致，消除潜在的版本冲突。
- **实现位置**：项目根目录 `pom.xml`

---

### ❌ 未实现 (按优先级排序)

#### 2. 并行化渲染驱动 (P0) ❌
- **细节**：利用 Java 8+ 的 `ParallelStream` 或 `CompletableFuture` 对表元数据进行并发处理。
- **逻辑**：在多核 CPU 环境下，将百级别表的生成耗时从秒级降低至毫秒级，协同处理 IO 与 CPU 密集型任务。

#### 10. 原子性生成保护 (P0) ❌
- **细节**：引入临时目录生成与最终替换机制。
- **逻辑**：生成过程中的任何异常均不会污染目标目录，只有全部渲染任务成功后才会更新输出结果，保证生成工程的原子性。

#### 11. 跨平台诊断工具 (EnvChecker) (P2) ❌
- **细节**：内置环境预检逻辑。
- **逻辑**：启动时自动检查 JDK 版本 (>=17)、系统字符集、目录写权限等关键指标，提供精准的故障诊断提示。

#### 8. 配置模块化 (Include Config) (P3) ❌
- **细节**：支持 `include: [sub-configs]` 语法。
- **逻辑**：允许将庞大的 `generator.yaml` 拆分为数据库配置、组件配置等多个模块，提高大型项目配置的可维护性。

#### 3. 数据源深度配置透传 (P3) ❌
- **细节**：在 `generator.yaml` 中开放 Druid 连接池的高级参数（如 `maxActive`, `minIdle`）。
- **逻辑**：针对大型数据库或复杂元数据场景，优化连接持有效率，防止在并发生成时连接枯竭。
