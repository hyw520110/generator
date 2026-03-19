# Generator CMD

代码生成器命令行版，提供交互式的命令行界面，支持快速生成前后台工程。

## 一、模块概述

`generator-cmd` 是代码生成器的命令行实现，其特点包括：

- **交互式配置**：启动后通过命令行提示输入数据库连接和生成配置。
- **快速模式**：支持通过命令行参数直接指定配置，无需人工交互，适合 CI/CD 自动化。
- **自动检测模式**：支持开发模式（源码运行）和部署模式（JAR 运行）。
- **多平台支持**：提供 Windows (`.bat`) 和 Linux/Unix (`.sh`) 启动脚本。
- **灵活启动**：支持指定自定义配置文件和远程调试端口。

## 二、目录结构

```
cmd/
├── src/main/java/      # 命令行生成器源码
├── logs/               # 运行日志及临时文件
├── run.sh              # Linux/Unix 启动脚本
├── run.bat             # Windows 启动脚本
└── pom.xml             # Maven 项目配置
```

## 三、快速开始

### 3.1 编译打包

在项目根目录或 `cmd` 目录下执行：

```bash
mvn clean package
```

### 3.2 启动生成器

#### 开发模式 (源码环境)

确保已执行 `mvn compile`，然后运行：

```bash
./run.sh
```

#### 部署模式 (生产环境)

将打包后的 `generator-cmd-1.0.1-SNAPSHOT.jar` 放入 `lib` 目录，然后运行：

```bash
./run.sh
```

### 3.3 常用命令参数

```bash
# 交互式模式（传统方式）
./run.sh                        # 自动检测模式启动，交互式输入配置
./run.sh 5005                   # 启动 + 调试端口
./run.sh my.yaml                # 启动 + 自定义配置文件
./run.sh my.yaml 5005           # 启动 + 自定义配置 + 调试端口

# 快速模式（新增，适合自动化）
./run.sh --help                 # 查看帮助信息

# 最小化参数（使用默认值）
./run.sh --quick \
  --db-password 123456 \
  --db-name mydb

# 完整参数
./run.sh --quick \
  --db-type mysql \
  --db-ip 192.168.1.100 \
  --db-port 3306 \
  --db-user root \
  --db-password 123456 \
  --db-name mydb \
  --output-dir /output/demo \
  --package com.example.demo

# 快速模式生成指定表
./run.sh --quick \
  --db-password 123456 \
  --db-name mydb \
  --tables user_info,order_info \
  --output-dir /output/demo \
  --package com.example.demo

# 使用配置文件（命令行参数可覆盖配置）
./run.sh --config my-config.yaml --quick
```

## 四、配置说明

### 4.1 交互式模式（传统方式）

启动后，程序会依次提示输入：

1. **数据库类型** (MySQL, Oracle, PostgreSQL, SQL Server)
2. **JDBC URL**
3. **用户名**
4. **密码**
5. **输出目录** (默认当前目录下的 `output`)
6. **根包名** (如 `com.example.demo`)

如果不输入直接回车，将使用默认配置。

### 4.2 快速模式（新增）

通过命令行参数直接指定所有必要配置，无需人工交互：

#### 必要参数

| 参数 | 说明 |
|------|------|
| `--db-password` | 数据库密码 |
| `--db-name` | 数据库名称 |

#### 可选参数（有默认值）

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--db-type` | 数据库类型 | `mysql` |
| `--db-ip` | 数据库 IP 地址 | `127.0.0.1` |
| `--db-port` | 数据库端口 | 各数据库默认端口 |
| `--db-user` | 数据库用户名 | `root` |
| `--output-dir` | 生成代码的输出目录 | 配置文件中的值 |
| `--package` | 项目根包名 | 配置文件中的值 |
| `--tables` | 要生成的表名（多表逗号分隔，或 `*` 生成所有表） | `*` |
| `--modules` | 工程模块（多模块逗号分隔） | `api,app` |

#### 可选参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--table-prefix` | 要移除的表前缀（多前缀逗号分隔） | 无 |
| `--components` | 组件预设：`standard`(标准版), `full`(全家桶) | `standard` |
| `--file-override` | 文件是否覆盖 | `true` |
| `--open-dir` | 生成完成后是否打开目录 | `false` |
| `--description` | 项目描述 | 配置文件中的值 |
| `--config` | 使用 YAML 配置文件（优先级低于命令行参数） | 无 |

## 五、技术栈

- **Java 1.8+**
- **Maven** (构建工具)
- **Generator Core** (核心引擎)
- **Logback** (日志管理)
