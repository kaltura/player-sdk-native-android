#!/bin/bash

set -ue

./gradlew :kalturaPlay:assembleRelease
./gradlew :testapp:assembleRelease
./gradlew :kalturaPlay:assembleDebug
./gradlew :testapp:assembleDebug

if [ "$BUILD_DEMOS" == "true" ]; then
  ./gradlew -p KalturaDemos assembleDebug
fi
