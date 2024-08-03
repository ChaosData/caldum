
JAVA_HOME=~/.jdks/corretto-11.0.23

java -javaagent:../basehook/build/libs/basehook-all-vl.jar -cp ./build/libs/testapp-tests.jar trust.nccgroup.caldumtest.Main &
MAIN_PID=$! ;
{
  sleep 2 ;
  echo ">curl 1"
  curl -s http://127.0.0.1:7777/premainedbasehooktest ;
  echo ">unload"
  java -jar ../basehook/build/libs/vl.jar "${MAIN_PID}" __embedded_agent__ -- unload ;

  echo ">curl 2"
  curl -s http://127.0.0.1:7777/detachbasehooktest ;
  sleep 2 ;

  echo ">curl 3"
  java -jar ../basehook/build/libs/vl.jar "${MAIN_PID}" ../basehook/build/libs/basehook-all.jar;
  curl -s http://127.0.0.1:7777/reattachbasehooktest ;
} &
wait "${MAIN_PID}";
