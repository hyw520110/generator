#!/bin/bash

pidFile=`cat ${project.build.finalName}.pid | awk '{print $1}'`

pidFile=`ps -aef | grep $pidFile | awk '{print $2}' |grep $pidFile`
if [ ${pidFile} ]; then
        kill -9 $pidFile
fi
