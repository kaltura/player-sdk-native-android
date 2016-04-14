#!/bin/bash

./gradlew :kalturaPlay:assembleRelease
./gradlew :testapp:assembleRelease
./gradlew :kalturaPlay:assembleDebug
./gradlew :testapp:assembleDebug

if [ "$BUILD_DEMOS" == "true" ]; then
  cd KalturaDemos
  ./gradlew assembleDebug
  cd -
fi

