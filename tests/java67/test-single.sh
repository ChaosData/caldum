#!/bin/sh

# Copyright 2024 Jeff Dileo
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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

DEBUGARG=""
if [ "$DEBUG" = "1" ]; then
  DEBUGARG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
fi

if [ "$TEST" = "premain" ]; then
  # premain using vl embed
  docker run --rm -it \
    -v "${SCRIPTDIR}/workdir:/workdir" \
    -v "${SCRIPTDIR}/build:/build:ro" \
    -w /workdir \
    -p 127.0.0.1:5005:5005 \
    "${IMAGE}" \
    java -javaagent:/build/libs/java67-all-vl.jar ${DEBUGARG} -cp /build/libs/java67-tests.jar org.junit.runner.JUnitCore trust.nccgroup.caldumtest.RunAllTests
elif [ "$TEST" = "agentmain" ]; then
  # agentmain using vl to load java67-all.jar which doesn't actually have caldum/byte-buddy in it
  docker run --rm -it \
    -v "${SCRIPTDIR}/workdir:/workdir" \
    -v "${SCRIPTDIR}/build:/build:ro" \
    -w /workdir \
    -p 127.0.0.1:5005:5005 \
    "${IMAGE}" \
    sh -c "java ${DEBUGARG} -cp /build/libs/java67-tests.jar trust.nccgroup.caldumtest.PausedMain & MAIN_PID=\$! ; \
    { \
      sleep 2 && \
      java -jar /build/libs/vl.jar \"\${MAIN_PID}\" /build/libs/java67-all.jar ; \
      curl -s http://127.0.0.1:7777 ; \
    } & \
    wait \"\${MAIN_PID}\"; "
fi
