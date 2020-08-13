#[[#!/bin/sh

CURRENT_DIR=`cd $(dirname $0); pwd -P`
BASE_DIR=${CURRENT_DIR%/*}

PIDS=`ps -ef|grep $BASE_DIR | grep -v grep |awk '{print $2}'` 
[ -n "$PIDS" ] && echo kill $CURRENT_DIR process:$PIDS  && kill -9 $PIDS 1>/dev/null 2>&1 | exit 0]]#