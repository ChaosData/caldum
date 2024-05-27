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

CALDUMDIR=`readlink -f "${SCRIPTDIR}"`

cd "${CALDUMDIR}/caldum"
./gradlew clean
./gradlew

cd "${CALDUMDIR}/embeddedagentplugin"
./gradlew clean
./gradlew

cd "${CALDUMDIR}/vulcanloader"
./gradlew clean
./gradlew
