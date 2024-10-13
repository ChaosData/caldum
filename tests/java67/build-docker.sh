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

CALDUMDIR=`readlink -f "${SCRIPTDIR}/../../"`

BUILD_IMAGE="eclipse-temurin:11-jdk"

if [ "$#" -eq "1" ]; then
  BUILD_IMAGE="$1"
fi

docker run --rm \
  -v "${CALDUMDIR}:/caldum" \
  -v "${CALDUMDIR}/build/m2:/root/.m2" -v "${CALDUMDIR}/build/gradle:/root/.gradle" \
  -w "/caldum/tests/java67" "${BUILD_IMAGE}" "./build.sh"
