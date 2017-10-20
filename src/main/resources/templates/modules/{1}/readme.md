# 说明

#if($!ZOOKEEPER)
#[[## 初始化：]]#
- 利用zookeeper客户端zkCli导入zookeeper.data脚本数据
- 或者执行ZkTool类,从zookeeper.data脚本文件或application.yml配置文件,导入初始到zookeeper
			
#end

#if($!SPRINGBOOT)
#[[## 启动：]]#
执行Booter类
#end

