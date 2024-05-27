#!/bin/sh
set -e
set -x

SCRIPT=$0
cd `dirname $SCRIPT`
SCRIPT=`basename $SCRIPT`
while [ -L "$SCRIPT" ]
do
  SCRIPT=`readlink $SCRIPT`
  cd `dirname $SCRIPT`
  SCRIPT=`basename $SCRIPT`
done
SCRIPTDIR=`pwd -P`

TEST=$1
IMAGE=$2

cd "${SCRIPTDIR}"

if [ "$TEST" = "premain" ]; then
  docker run --rm -it -v "${SCRIPTDIR}/build:/build:ro" "${IMAGE}" \
    java -javaagent:/build/libs/java89-all-vl.jar -cp /build/libs/java89-tests.jar org.junit.runner.JUnitCore trust.nccgroup.caldumtest.RunAllTests
elif [ "$TEST" = "agentmain" ]; then
  docker run --rm -it -v "${SCRIPTDIR}/build:/build:ro" "${IMAGE}" \
    sh -c 'java -cp /build/libs/java89-tests.jar trust.nccgroup.caldumtest.PausedMain 2>&1 > /tmp/log & MAIN_PID=$! ; \
    { \
      sleep 1 && \
      java -jar /build/libs/vl.jar "${MAIN_PID}" /build/libs/java89-all.jar ; \
      curl -s http://127.0.0.1:7777 ; \
    } & \
    wait "${MAIN_PID}"; \
    cat /tmp/log'
fi
