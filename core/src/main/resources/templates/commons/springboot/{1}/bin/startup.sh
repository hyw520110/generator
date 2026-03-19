#!/bin/bash

KEYWORD="${project.name}.jar"

CURRENT_DIR=`cd $(dirname $0); pwd -P`
#[[BASE_DIR=${CURRENT_DIR%/*}]]#
cd $BASE_DIR

[ ! -d "$BASE_DIR/logs" ] && mkdir $BASE_DIR/logs

JAVA_OPTS="-server -Xms${JAVA_XMS:-512M} -Xmx${JAVA_XMX:-512M} -Xmn${JAVA_XMN:-200M} -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128M -Xss256k -XX:ParallelGCThreads=20 -XX:+UseConcMarkSweepGC -Djava.io.tmpdir=${TMP_DIR:-/logs/}"
#if($SENTINEL)
SENTINEL_OPTS=-Dproject.name=${project.name} -Dcsp.sentinel.dashboard.server=${dashboard.server} -Dcsp.sentinel.api.port=${api.port}  -Dcsp.sentinel.app.type=${app.type}
#end
#if($SKYWALKING)
SKYWALKING_ADDR="$skywalking.addr"
SKYWALKING_OPTS=""
[ -n "$SKYWALKING_ADDR" ] && SKYWALKING_ADDR="-javaagent:$skywalking.agent-home/${project.name}/skywalking-agent.jar -Dskywalking.agent.service_name=${project.name} -Dskywalking.collector.backend_service=$SKYWALKING_ADDR"
#end
[ -n "$1" ] && DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$1"

count=`ps -ef |grep java|grep $BASE_DIR|grep -v grep|wc -l`
#[[if [ $count != 0 ];then]]#
    echo "$KEYWORD is running..."
else
	echo "nohup java $JAVA_OPTS $DEBUG $SENTINEL_OPTS $SKYWALKING_ADDR -jar  $BASE_DIR/lib/$KEYWORD  > /logs/boot.log 2>&1 &"
    nohup java $JAVA_OPTS $DEBUG $JMX  $SENTINEL_OPTS $SKYWALKING_ADDR -jar  $BASE_DIR/lib/$KEYWORD  > /logs/boot.log 2>&1 &
fi
