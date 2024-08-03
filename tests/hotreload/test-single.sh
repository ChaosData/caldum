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

# this file is named test-single.sh for the test.sh machinery
# but unlike other tests fitting that mold, it runs a much more complicated
# test, in which it sets up a test app with an initial premain hook
# kicks it off via curl, then starts doing unloads/reloads/hot reloads
# and using curl to kick off individual tests for the stage

#unused
TEST=$1

IMAGE=$2

cd "${SCRIPTDIR}"

DEBUGARG=""
if [ "$DEBUG" = "1" ]; then
  DEBUGARG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
fi

docker run --rm -it \
  -v "${SCRIPTDIR}/workdir:/workdir" \
  -v "${SCRIPTDIR}/testapp/build:/build:ro" \
  -v "${SCRIPTDIR}/basehook/build:/basehook/build:ro" \
  -w /workdir \
  -p 127.0.0.1:5005:5005 \
  "${IMAGE}" \
  sh -c "java ${DEBUGARG} \
    -javaagent:/basehook/build/libs/basehook-all-vl.jar \
    -cp /build/libs/baseapp-tests.jar trust.nccgroup.caldumtest.PausedMain & MAIN_PID=\$! ; \
  { \
    sleep 2 && \
    curl -s http://127.0.0.1:7777/premainedbasehooktest ; \
    java -jar /basehook/build/libs/vl.jar \"\${MAIN_PID}\" /basehook/build/libs/basehook-all.jar -- unload; \
    curl -s http://127.0.0.1:7777/detachbasehooktest ; \
    sleep 2 && \
    java -jar /basehook/build/libs/vl.jar \"\${MAIN_PID}\" /basehook/build/libs/basehook-all.jar; \
    curl -s http://127.0.0.1:7777/reattachbasehooktest ; \
  } & \
  wait \"\${MAIN_PID}\"; "

