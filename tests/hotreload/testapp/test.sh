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

echo ">build testapp"
./gradlew
rm -f /tmp/caldum.log

# JAVA_HOME=~/.jdks/corretto-11.0.23
echo ">build hook (#1)"
cd "${SCRIPTDIR}/../basehook"
#JAVA_HOME=~/.jdks/corretto-11.0.23 ./gradlew -Phookversion=1
./build.sh 1

cd "${SCRIPTDIR}"
echo ">java with embedded agent premain"

java -javaagent:../basehook/build/libs/basehook-all-vl.jar -cp ./build/libs/testapp-tests.jar trust.nccgroup.caldumtest.Main &
MAIN_PID=$!
sleep 2
#sleep 8

TESTS=$1

(
  if [ $TESTS -ge 1 ]; then
    echo ">curl 1" 
    curl -s http://127.0.0.1:7777/premainedbasehooktest || echo ""
  fi
  if [ $TESTS -ge 2 ]; then
    sleep 1 ;
    echo ">unload" ;
    java -jar ../basehook/build/libs/vl.jar "${MAIN_PID}" __embedded_agent__ -- unload ;
    sleep 1 ;
    echo ">curl 2" ;
    curl -s http://127.0.0.1:7777/detachbasehooktest || echo ""
  fi
  if [ $TESTS -ge 3 ]; then
    sleep 1 ;
    echo ">re-attach" &&
    java -jar ../basehook/build/libs/vl.jar "${MAIN_PID}" ../basehook/build/libs/basehook-all.jar &&
    sleep 1 ;
    echo ">curl 3" &&
    curl -s http://127.0.0.1:7777/reattachbasehooktest || echo ""
  fi
  if [ $TESTS -ge 4 ]; then
    echo ">rebuild hook (#2)" ;
    cd "${SCRIPTDIR}/../basehook" ;
    ./build.sh 2 ;
    cd "${SCRIPTDIR}/" ;
    echo ">re-attach newer version of hook" ;
    java -jar ../basehook/build/libs/vl.jar "${MAIN_PID}" ../basehook/build/libs/basehook-all.jar ;
    sleep 1 ;
    echo ">curl 4" &&
    curl -s http://127.0.0.1:7777/reattachbasehooktest || echo ""
  fi
  if [ $TESTS -ge 5 ]; then
    echo ">rebuild hook (#3)" ;
    cd "${SCRIPTDIR}/../basehook" ;
    ./build.sh 3 ;
    cd "${SCRIPTDIR}/" ;
    echo ">re-attach newer version of hook" ;
    java -jar ../basehook/build/libs/vl.jar "${MAIN_PID}" ../basehook/build/libs/basehook-all.jar ;
    sleep 1 ;
    echo ">curl 5" &&
    curl -s http://127.0.0.1:7777/reattachbasehooktest || echo ""
  fi
) &
wait "${MAIN_PID}";
