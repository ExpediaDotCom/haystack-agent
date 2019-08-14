#!/bin/bash

[ -z "$JAVA_XMS" ] && JAVA_XMS=1024m
[ -z "$JAVA_XMX" ] && JAVA_XMX=1024m

set -e
JAVA_OPTS="${JAVA_OPTS} \
-XX:+UseConcMarkSweepGC \
-XX:+UseParNewGC \
-Xmx${JAVA_XMX} \
-Xms${JAVA_XMS} \
-Dapplication.name=${APP_NAME} \
-Dapplication.home=${APP_HOME}"

if [ -z "${HAYSTACK_AGENT_CONFIG_FILE_PATH}" ]; then
    exec java ${JAVA_OPTS} -jar "${APP_HOME}/${APP_NAME}.jar" --config-provider file --file-path /app/bin/default.conf
else
    exec java ${JAVA_OPTS} -jar "${APP_HOME}/${APP_NAME}.jar" --config-provider file --file-path ${HAYSTACK_AGENT_CONFIG_FILE_PATH}
fi