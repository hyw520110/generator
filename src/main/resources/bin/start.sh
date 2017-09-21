#!/bin/bash

url=ftp://192.168.40.113/dev-tools/JDK/jre-8u144-windows-x64.tar.gz
url2=http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jre-8u144-windows-x64.tar.gz?AuthParam=1504671349_880553e661c425ff6681836f22ae90df
#压缩包内的文件夹名
dirName=jre1.8.0_144
#脚本当前目录
CURRENT_DIR=$(cd "$(dirname "$0")"; pwd)

function downloadJdk()
{
	if [ -d "$dirName" ];then
		if [ ! -f jre8.tar.gz ];then
			echo begin download $dirName...
			wget $url -O jre8.tar.gz || wget $url2 -O jre8.tar.gz
		fi
		tar zxvf jre8.tar.gz
		rm -rf jre8.tar.gz
	fi
	export path=$CURRENT_DIR/%dirName%/bin:%path%
	startService
}

function startService()
{
	echo start ${project.build.finalName} service...
	java -jar ../lib/${project.build.finalName}.jar  
}

#执行java命令看执行是否成功，执行失败：下载jre设置环境变量，启动服务;执行成功:直接开启服务
java -version && startService || downloadJdk



 

