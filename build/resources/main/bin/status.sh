#!/bin/bash

pidFile=`cat ${project.build.finalName}.pid | awk '{print $1}'`
pidFile=`ps -aef | grep $pidFile | awk '{print $2}' |grep $pidFile`
if [ ${pidFile} ]; then
        echo ${project.build.finalName} is running.
else
        echo ${project.build.finalName} is NOT running.
fi