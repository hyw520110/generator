#!/bin/sh

KEYWORD="${project.name}.jar"

CURRENT_DIR=`cd $(dirname $0); pwd -P`
BASE_DIR=${CURRENT_DIR%/*}
cd $BASE_DIR

[ ! -d "$BASE_DIR/logs" ] && mkdir $BASE_DIR/logs

JAVA_OPTS="-server -Xms512M -Xmx512M -Xmn200M -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M -Xss256k -XX:ParallelGCThreads=20 -XX:+UseConcMarkSweepGC -Djava.io.tmpdir=/logs/"

[ -n "$1" ] && DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$1"

count=`ps -ef |grep java|grep $BASE_DIR|grep -v grep|wc -l`
if [ $count != 0 ];then
    echo "$KEYWORD is running..."
else
        
	java $JAVA_OPTS $DEBUG $JMX -jar  ./lib/$KEYWORD
fi