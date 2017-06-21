说明：
== 
代码生成器

- 支持代码、配置、依赖的生成
- 支持多模块工程的生成,模块名可配置
- 模板自定义支持,支持组件扩展

TODO LIST:
==
1. 各个数据库适配、验证、配置调整
2. 各个组件适配(JPA、DUBBO...)、配置文件、测试用例
3. springmvc增删改查及页面的生成 

快速开始:
==
1. 修改配置文件generator.yaml(黑体为必须修改项 ,其他均为可选修改项)：

- **修改数据源配置**
- 修改全局配置
	- **定义输出目录(outputDir)**,最后一个子目录为项目名
	- 是否清空输出目录(delOutputDir)默认为false
 	- 定义是否覆盖生成(默认false)
 	- 定义作者(author)
- 修改生成策略
	- **定义根包(rootPackage)**
	- 定义移除的表前缀tablePrefix
	- 是否生成构建脚本:pom.xml(配置MAVEN)、build.gradle(配置GRADLE)、不生成(不配置)
2. 执行Generator的main方法	
	
配置说明:
==
具体看配置文件generator.yaml注释说明

自定义
==
dao、service、controller层的父类自定义，在配置文件中修改即可

默认自定义，复制templates目录重命名,在配置文件中修改模板名即可



