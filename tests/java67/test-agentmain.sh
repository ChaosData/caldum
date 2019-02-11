#!/bin/sh

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

cd "${SCRIPTDIR}/../../vulcanloader" && ./gradlew shadowJar -Pno-tools && cp ./build/libs/vl.jar "${SCRIPTDIR}/build/libs/"
cd "${SCRIPTDIR}"

./gradlew shadowJar testJar

#docker run -it -v "${SCRIPTDIR}/build:/build:ro" openjdk:6-jdk \
#  sh -c 'java -cp /build/libs/java67-tests.jar trust.nccgroup.caldumtest.PausedMain 2>&1 > /tmp/log & MAIN_PID=$! ; { sleep 1 && java -cp /docker-java-home/lib/tools.jar:/build/libs/vl.jar trust.nccgroup.vulcanloader.Main "${MAIN_PID}" /build/libs/java67.jar ; curl -s http://127.0.0.1:7777 ; } & wait "${MAIN_PID}"; cat /tmp/log'

docker run -it -v "${SCRIPTDIR}/build:/build:ro" openjdk:6-jdk \
  sh -c 'java -cp /build/libs/java67-tests.jar trust.nccgroup.caldumtest.PausedMain 2>&1 > /tmp/log & MAIN_PID=$! ; \
  { \
    sleep 1 && \
    java -cp /docker-java-home/lib/tools.jar:/build/libs/vl.jar trust.nccgroup.vulcanloader.Main "${MAIN_PID}" /build/libs/java67.jar ; \
    curl -s http://127.0.0.1:7777 ; \
  } & \
  wait "${MAIN_PID}"; \
  cat /tmp/log'

