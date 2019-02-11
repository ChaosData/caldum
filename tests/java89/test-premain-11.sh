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

./gradlew caldum-vl-embed testJar

docker run -it -v "${SCRIPTDIR}/build:/build:ro" openjdk:11-jdk \
  java -javaagent:/build/libs/java89-vl.jar -cp /build/libs/java89-tests.jar org.junit.runner.JUnitCore trust.nccgroup.caldumtest.RunAllTests
