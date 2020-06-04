# 说明：

代码生成器,主要功能：

- 支持主流关系型数据库(mysql、oracle、postgresql、sqlserver)
- 支持整库生成、指定部分表、反选表生成,支持数据库表复合主键的生成
- 支持构建脚本生成(gradle/maven/不生成)
- 支持多模块工程的生成,模块名可自定义配置
- 支持模板自定义,排版风格支持自定义,java类注释支持自定义(更改模板文件comment.vm)
- 支持组件扩展，组件特性自定义及特性扩展


# 生成器配置说明:

配置文件generator.yaml,主要配置包括(粗体必须配置):

- **数据源配置**：driverClassName、URL、username、pwd
- **全局配置**：生成目录outputDir、是否清空输出目录delOutputDir、是否覆盖生成fileOverride、技术组件components
- **生成策略配置**：根包rootPackage
- 组件配置:如组件选配了dubbo、zipkin、zookeeper、rocketmq、redis等,需指定组件所需的地址,否则一般保持默认即可

具体配置项说明见配置文件注释说明

# 快速开始:

修改配置文件generator.yaml(必须修改项 ,其他均为可选修改项)：

			  
	url: jdbc:mysql://localhost:3306/test?useUnicode=true&autoReconnect=true&characterEncoding=UTF-8
	username: root
	pwd: 123456
    
	outputDir: /output/test
	components: [MYBATIS,SPRINGMVC,SPRINGBOOT,DUBBO,ZIPKIN,ZOOKEEPER,ROCKETMQ,SWAGGER2]
	
	rootPackage: com.test
其中components组件配置根据实际需要选取（如需要生成页面，配置THYMELEAF即可），选取后配置相应组件地址信息(redis、dubbo、zookeeper、rocketmq地址)

默认生成单模块工程，也可生成多模块工程，配置modules即可，如生成api和app工程配置：

	modules: [ 'api','app']

可选两种方式执行（二选一即可）：1、通过git下载源码执行，2、下载zip包执行命令脚本.

## 1. 源码方式：


导入生成的源码到IDE(安装配置好maven/gradle),执行Generator的main方法.


## 2. 命令脚本：
	
- 分支下载zip包或通过源码构建获取zip包:在工程根目录下执行：mvn clean package，在target目录下获取zip包 
- 解压zip包，修改配置文件generator.yaml
- 执行bin目录下的命令脚本start.bat,执行代码生成

## 启动服务
	
通过源码或命令脚本生成代码后，资源管理器会自动打开生成路径,初始化数据或配置之后就可以启动服务，启动服务可通过源码执行或脚本执行

### 初始化

如配置的组件中不包含zookeeper，则默认是配置文件yml，注意查看配置文件(默认目录src/main/resources目录下)，根据需要修改配置或完全保持默认配置

如配置的组件中包含zookeeper，则初始化配置数据
	- 进入生成的源码根目录下的app目录,打开src/test/resources/zookeeper.data全选复制
	- cmd进入zookeeper的bin目录下执行 zkCli -server localhost:2181 粘贴 回车

### 源码执行

- 把生成的源码导入IDE(maven/gradle工程)
- 执行Boot.java启动服务，服务启动完成后自动打开浏览器(profile为dev时)	
		
### 脚本执行

傻瓜化启动服务,支持无jre/jdk环境(自动下载jre并配置临时环境变量)启动运行服务

以模块配置api、app为例： 

- 进入parent目录，执行package.bat
- 进入app目录下的target目录，解压zip包
- 进入解压目录,如需修改配置,则先修改配置,否则直接进入bin目录,执行start.bat启动服务(执行debug.bat调试启动),服务启动完成后自动打开浏览器(profile为dev时)


# 自定义
- 自定义工程构建工具：maven(pom.xml)、gradle(build.gradle)、无

- 自定义工程模块，支持生成单工程和多模块工程，多模块工程，工程模块名可自定义

- 父类自定义：dao、service、controller层的父类自定义，在配置文件中指定即可

- 自定义文件名： entity、dao、service、contrller层的类名及配置文件名支持自定义生成

- 自定义模板:

	- 复制templates目录重命名,修改模板文件
	- 在配置文件中修改模板目录名

- 扩展组件:

	- 在配置文件中指定模块名数组(modules),如模块配置为[api,app],已有的组件配置为[mybatis,springmvc],新增dubbo配置:[mybatis,springmvc,dubbo]
	- 在模板目录(templates)下新建目录，目录名为{模块数组index},如dubbo的配置文件在app模块中，则新建{2}文件夹,在文件夹下新建（文件夹）模板文件
	- 在配置文件中新增组件配置(非必须),配置模板所需的配置信息

技术组件：

- zookeeper
- redis集群及切片缓存
- dubbo
- zipkin实时数据追踪(需在生成器配置文件中指定jdk为1.8)

	
	
# TODO LIST:

1. 各主流关系型数据库适配验证、配置调整
2. 各个组件适配(JPA、DUBBO...)、配置文件以及测试用例的生成
3. 数据验证及国际化(存放错误消息,便于更新维护)支持
4. 增加分布式消息中间件
5. 生成器交互式脚本
6. zookeeper数据初始化导入
7. redis切片緩存key优化：演示功能，设置过于简单 ,需考虑唯一性以及序列化与反序列化

# FAQ:

1. 执行Generator类的main方法报错 ：

	java.io.IOException:Stream closed 	
- 解决：
	- 找不到generator.yaml配置文件，确认是否编译(target/classes下是否有该文件)	
	- 有编译错误，导致沒有自动编译，如src/main/resources目录下的模板文件有编译错误，直接设置忽略即可

2. 启动服务(生成的应用服务)报错：

	ClassCastException: java.lang.UnsupportedClassVersionError cannot be cast to [Ljava.lang.Object;
- 原因：
	- 指定生成jdk1.8配置，而运行环境的jdk版本低于1.8
- 解决：
	- 安装jdk1.8/jre1.8,并设置环境变量
	- 或者在pom中指定jdk为1.7，并去掉zipkin数据追踪依赖、mq消息中间件依赖