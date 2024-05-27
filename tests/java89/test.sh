#!/bin/sh

set -e
SCRIPT=$0
cd `dirname $SCRIPT`
SCRIPT=`basename $SCRIPT`

# premain
# agentmain
TESTS="premain agentmain"
if [ "$#" -ne "0" ]; then
  if [ "$#" -gt "1" ]; then
    if [ "$2" = "--" ]; then
      TESTS="$1"
      shift 2
    fi
  fi
fi

# eclipse-temurin:11-jdk
# eclipse-temurin:17-jdk
# eclipse-temurin:21-jdk
# ibm-semeru-runtimes:open-11-jdk
# ibm-semeru-runtimes:open-17-jdk
# ibm-semeru-runtimes:open-21-jdk
if [ "$#" -eq "0" ]; then
  set -- "$@" "eclipse-temurin:11-jdk" "eclipse-temurin:17-jdk" "eclipse-temurin:21-jdk" \
              "ibm-semeru-runtimes:open-11-jdk" "ibm-semeru-runtimes:open-17-jdk" "ibm-semeru-runtimes:open-21-jdk"
fi

IMAGES=$@

#for image in $IMAGES; do
#  echo "running tests '${TESTS}' for ${image}"
#done
#exit 0

while [ -L "$SCRIPT" ]
do
  SCRIPT=`readlink $SCRIPT`
  cd `dirname $SCRIPT`
  SCRIPT=`basename $SCRIPT`
done
SCRIPTDIR=`pwd -P`

./gradlew clean
#./gradlew shadowJar testJar
./gradlew shadowJar caldum-vl-embed testJar

cd "${SCRIPTDIR}/../../vulcanloader" && ./gradlew shadowJar -Pno-tools && cp ./build/libs/vl.jar "${SCRIPTDIR}/build/libs/"
cd "${SCRIPTDIR}"

for TEST in $TESTS; do
  for IMAGE in $IMAGES; do
    echo "Running ${TESt} test against ${IMAGE}"
    if [ "$TEST" = "premain" ]; then
      docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${IMAGE}" \
        java -javaagent:/build/libs/java89-all-vl.jar -cp /build/libs/java89-tests.jar org.junit.runner.JUnitCore trust.nccgroup.caldumtest.RunAllTests
    elif [ "$TEST" = "agentmain" ]; then
      docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${IMAGE}" \
        sh -c 'java -cp /build/libs/java89-tests.jar trust.nccgroup.caldumtest.PausedMain 2>&1 > /tmp/log & MAIN_PID=$! ; \
        { \
          sleep 1 && \
          java -jar /build/libs/vl.jar "${MAIN_PID}" /build/libs/java89-all.jar ; \
          curl -s http://127.0.0.1:7777 ; \
        } & \
        wait "${MAIN_PID}"; \
        cat /tmp/log'
    fi
  done
done
