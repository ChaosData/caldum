#!/bin/sh

# Copyright 2019 NCC Group
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

docker run -it -v "${SCRIPTDIR}/build:/build:ro" openjdk:6-jdk \
  java -javaagent:/build/libs/java67-vl.jar -cp /build/libs/java67-tests.jar org.junit.runner.JUnitCore trust.nccgroup.caldumtest.RunAllTests
