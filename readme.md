说明：
== 
代码生成器

- 支持常见主流数据库
- 支持代码、配置、依赖的生成
- class类注释支持自定义(更改模板文件comment.vm)
- 支持构建脚本生成(gradle/maven/不生成)
- 支持多模块工程的生成,模块名可自定义配置
- 支持模板自定义,排版风格支持自定义
- 支持组件扩展，组件特性自定义及特性扩展
- 支持jdk1.8,向下兼容1.5及以上版本(jdk1.8以下导入ide会编译报错，更改jre为1.7或1.5以上版本，编译报错的代码注释掉即可)

TODO LIST:
==

1. 复合主键支持 
2. 各个数据库适配、验证、配置调整
3. 各个组件适配(JPA、DUBBO...)、配置文件以及测试用例的生成
4. springmvc增删改查方法及页面的生成 
5. 命令脚本执行生成器

快速开始:
==
1. 修改配置文件generator.yaml(黑体为必须修改项 ,其他均为可选修改项)：

- **修改数据源配置**
- 修改全局配置
	- **定义输出目录(outputDir)**,最后一个子目录为项目名
	- **是否清空输出目录(delOutputDir)默认为false**,工程已存在的情况下，此配置项谨慎使用(不要配置为true)
 	- **定义是否覆盖生成(默认false)**,工程文件已存在的情况下，此配置项谨慎使用(不要配置为true)
 	- 定义作者(author)
 	- 定义版权(copyright)
- 修改生成策略
	- **定义根包(rootPackage)**
	- 定义移除的表前缀tablePrefix
	- 是否生成构建脚本:pom.xml(配置MAVEN)、build.gradle(配置GRADLE)、不生成(不配置)
2. 安装配置好maven/gradle导入IDE,执行Generator的main方法.或执行命令脚本	
	
配置说明:
==
- 配置文件generator.yaml包含数据源配置、全局配置、生成策略配置，具体配置说明见配置文件配置项注释说明

- 组件特性配置文件见各组件配置文件,组件配置目录为:src/main/resources/conf

自定义
==
- 自定义工程构建工具：maven(pom.xml)、gradle(build.gradle)、无

- 自定义工程模块，支持生成单工程和多模块工程，多模块工程，工程模块名可自定义

- 父类自定义：dao、service、controller层的父类自定义，在配置文件中指定即可

- 自定义文件名： entity、dao、service、contrller层的类名及配置文件名支持自定义生成

- 自定义模板:

	- 复制templates目录重命名,修改模板文件
	- 在配置文件中修改模板目录名

- 扩展组件:

	- 在配置文件中指定模块名数组(modules),如已有的组件配置为[mybatis,springmvc],新增dubbo配置:[mybatis,springmvc,dubbo]
	- 在模板目录(templates)下新建目录，目录名为{数组index},如{2}
	- 在配置目录(conf)下新增组件配置文件,如dubbo.yaml,配置模板文件所需的配置数据
