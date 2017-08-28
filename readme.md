说明：
== 
代码生成器,主要功能包括：

- 支持常见主流数据库
- 数据库连接池：密码配置支持加密，支持SQL注入拦截，慢SQL记录，连接池监控等
- 支持代码、配置、依赖的生成,支持复合主键的生成
- class类注释支持自定义(更改模板文件comment.vm)
- 支持构建脚本生成(gradle/maven/不生成)
- 支持多模块工程的生成,模块名可自定义配置
- 支持模板自定义,排版风格支持自定义
- 支持组件扩展，组件特性自定义及特性扩展
- 支持jdk1.8,向下兼容1.5及以上版本(jdk1.8以下导入ide会编译报错，更改jre为1.7或1.5以上版本，编译报错的代码注释掉即可)



快速开始:
==
有兩種方式執行（二選一即可）：一種方式是通過git下載源碼執行，一種是下載zip執行命令腳本.

1. 源碼方式：

	- 修改配置文件generator.yaml(黑体为必须修改项 ,其他均为可选修改项)：

		- **修改数据源配置**
			- 如配置明文密码，直接配置好驱动类、url、用户名、明文密码即可
			- 如配置密文密码,执行以下命令(命令执行输出：公钥(publicKey)、私钥(privateKey)、密文密码)：					
				- java -cp %M2_REPO%\com\alibaba\druid\1.1.2\druid-1.1.2.jar com.alibaba.druid.filter.config.ConfigTools 123456
				- 密码配置为以上命令产生的密文密码
				- 配置filters为: config	
				- 配置连接属性(connectionProperties)为：config.decrypt=true;config.decrypt.key=${publicKey}			
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
	- 安装配置好maven/gradle导入IDE,执行Generator的main方法.


2. 命令腳本：
	
	- 分支下載zip或通過源碼構建獲取zip包,在工程根目錄下執行構建命令：mvn clean package，在target目錄下獲取zip包 
	- 解壓zip包，修改配置文件generator.yaml
	- 执行bin目錄下的命令脚本	
	
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

	- 在配置文件中指定模块名数组(modules),如模塊配置為[api,app],已有的组件配置为[mybatis,springmvc],新增dubbo配置:[mybatis,springmvc,dubbo]
	- 在模板目录(templates)下新建目录，目录名为{模塊数组index},如dubbo的配置文件在app模塊中，則新建{2}文件夾,在文件夾下新建（文件夾）模板文件
	- 在配置目录(conf)下新增组件配置文件,如dubbo.yaml配置模板所需的配置数据，如模板不需要配置數據，可不用新建組件配置文件

TODO LIST:
==

2. 各主流關係型数据库适配、验证、配置调整
3. 各个组件适配(JPA、DUBBO...)、配置文件以及测试用例的生成
4. springmvc增删改查方法及页面的生成 


FAQ:
==

eclipse打開pom.xml報錯：

	org.apache.maven.archiver.MavenArchiver.getManifest(org.apache.maven.project.MavenProject, org.apache.maven.archiver.MavenArchiveConfiguration) pom.xml
解決：

	- 編輯pom.xml更改maven-jar-plugin的版本號為2.6
	- 或者升級m2e extensions,添加url(安裝后重啟eclipse)：
		- https://otto.takari.io/content/sites/m2e.extras/m2eclipse-mavenarchiver/0.17.2/N/LATEST/
		- http://repo1.maven.org/maven2/.m2e/connectors/m2eclipse-mavenarchiver/0.17.2/N/LATEST/




執行Generator類的main方法報錯 ：

	java.io.IOException:Stream closed 	
解決：

	- 找不到generator.yaml配置文件，確認是否編譯(target/classes下是否有該文件)	
	- 有編譯錯誤，導致源碼沒有自動編譯，如src/main/resources目錄下的模板文件有編譯錯誤，直接設置忽略即可