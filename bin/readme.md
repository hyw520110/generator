说明：
== 
生成器工程

快速开始:
==
1. 修改配置文件generator.yaml(黑体为必须修改项 ,其他均为可选修改项)：

- **修改数据源配置**
- 修改全局配置
	- **定义输出目录(outputDir)**,最后一个子目录为项目名
 	- 定义是否覆盖生成(默认false)
 	- 定义作者(author)
- 修改生成策略
	- **定义根包(rootPackage)**
	- 定义移除的表前缀tablePrefix
	
2. 执行Generator的main方法	
	
配置说明:
==
具体看配置文件generator.yaml注释说明


TODO LIST:
==
1. 各个数据库适配、验证、配置调整
2. 各个组件适配(JPA、JPA-MONGODB...)
3. springmvc增删改查及页面的生成 


