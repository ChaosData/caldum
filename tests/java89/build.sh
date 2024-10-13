#!/bin/sh

# Copyright 2024 Jeff Dileo
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

cd "${SCRIPTDIR}/../../vulcanloader"
./gradlew shadowJar -Pno-tools
echo "${SCRIPTDIR}"

cd "${SCRIPTDIR}"

./gradlew clean
#./gradlew shadowJar testJar
./gradlew shadowJar caldum-vl-embed testJar

cp "${SCRIPTDIR}/../../vulcanloader/build/libs/vl.jar" "${SCRIPTDIR}/build/libs/"
