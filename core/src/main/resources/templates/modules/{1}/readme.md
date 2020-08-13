#[[# 说明]]#

#if($!ZOOKEEPER)
#[[## 初始化分布式配置数据(非必须)：]]#
建议把可能会调整的配置，初始化到zookeeper分布式配置中(个性化配置除外，如权重数据)，当应用集群部署时，方便集中管理配置数据，实现实时加载动态配置，避免逐台服务器手工修改配置文件

- 利用zookeeper客户端zkCli导入zookeeper.data脚本数据
- 或者执行ZkTool类,从zookeeper.data脚本文件或application.yml配置文件,导入初始到zookeeper
			
#end

#if($!SPRINGBOOT)
#[[## 启动：]]#
导入源码到IDE中执行Booter类

或执行：
	
	mvn clean package
打包源码,在target中找到应用zip包，解压任意路径,进入bin目录执行startup.sh启动
#end