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

cd "${SCRIPTDIR}/../../vulcanloader"
./gradlew shadowJar -Pno-tools
cp ./build/libs/vl.jar "${SCRIPTDIR}/build/libs/"

cd "${SCRIPTDIR}"

./gradlew clean
#./gradlew shadowJar testJar
./gradlew shadowJar caldum-vl-embed testJar

