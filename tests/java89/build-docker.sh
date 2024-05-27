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
cd "${SCRIPTDIR}"

CALDUMDIR=`readlink -f "${SCRIPTDIR}/../../"`

BUILD_IMAGE="eclipse-temurin:11-jdk"

if [ "$#" -eq "1" ]; then
  BUILD_IMAGE="$1"
fi

docker run --rm \
  -v "${CALDUMDIR}:/caldum" \
  -v "${CALDUMDIR}/build/m2:/root/.m2" -v "${CALDUMDIR}/build/gradle:/root/.gradle" \
  -w "/caldum/tests/java89" "${BUILD_IMAGE}" "./build.sh"
