#!/bin/sh

set -e
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
cd "${SCRIPTDIR}"

CALDUMDIR=`readlink -f "${SCRIPTDIR}/../"`

BUILD_IMAGE="eclipse-temurin:11-jdk"

# java67
# java89
#VERSIONS="java67 java89"
VERSIONS="java89"
if [ "$#" -ne "0" ]; then
  if [ "$#" -gt "1" ]; then
    if [ "$2" = "--" ]; then
      VERSIONS="$1"
      shift 2
    fi
  fi
fi

# premain
# agentmain
TESTS="premain agentmain"
if [ "$#" -ne "0" ]; then
  if [ "$#" -gt "1" ]; then
    if [ "$2" = "----" ]; then
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

#cd "${SCRIPTDIR}/../../vulcanloader" && ./gradlew shadowJar -Pno-tools && cp ./build/libs/vl.jar "${SCRIPTDIR}/build/libs/"
#cd "${SCRIPTDIR}"

"${CALDUMDIR}/build-docker.sh"

for VERSION in $VERSIONS; do
  echo "Building ${VERSION}"
  docker run --rm \
    -v "${CALDUMDIR}:/caldum" \
    -v "${CALDUMDIR}/build/m2:/root/.m2" -v "${CALDUMDIR}/build/gradle:/root/.gradle" \
    -w "/caldum/tests" "${BUILD_IMAGE}" "./${VERSION}/build.sh"

  for TEST in $TESTS; do
    for IMAGE in $IMAGES; do

      echo "Running ${VERSION} ${TEST} test against ${IMAGE}"
      "./${VERSION}/test-single.sh" "${TEST}" "${IMAGE}"

#    if [ "$TEST" = "premain" ]; then
#      docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${IMAGE}" \
#        java -javaagent:/build/libs/java89-all-vl.jar -cp /build/libs/java89-tests.jar org.junit.runner.JUnitCore trust.nccgroup.caldumtest.RunAllTests
#    elif [ "$TEST" = "agentmain" ]; then
#      docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${IMAGE}" \
#        sh -c 'java -cp /build/libs/java89-tests.jar trust.nccgroup.caldumtest.PausedMain 2>&1 > /tmp/log & MAIN_PID=$! ; \
#        { \
#          sleep 1 && \
#          java -jar /build/libs/vl.jar "${MAIN_PID}" /build/libs/java89-all.jar ; \
#          curl -s http://127.0.0.1:7777 ; \
#        } & \
#        wait "${MAIN_PID}"; \
#        cat /tmp/log'
#    fi
    done
  done
done
