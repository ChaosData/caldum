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

HOOKVERSION=$1

cd "${SCRIPTDIR}/../../../vulcanloader"
#./gradlew shadowJar -Pno-tools
./gradlew shadowJar
echo "${SCRIPTDIR}"

cd "${SCRIPTDIR}"

./gradlew "-Phookversion=${HOOKVERSION}" clean
./gradlew "-Phookversion=${HOOKVERSION}" shadowJar caldum-vl-embed testJar

cp "${SCRIPTDIR}/../../../vulcanloader/build/libs/vl.jar" "${SCRIPTDIR}/build/libs/"
