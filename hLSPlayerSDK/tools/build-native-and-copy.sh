#!/bin/bash

# This script builds the native part of Android-HLS-Lib, packages the output 
# and copies it to the correct path. It then copies the Java part of Android-HLS-Lib,
# so that it can be built as part of the overall Kaltura Player SDK.

if [ -z "$1" ]; then
  echo Please specify path to Android-HLS-lib > /dev/stderr
  exit 1
fi

# Resolve absolute path
resolveDir() {
  cd "$1"; pwd;
}


KALTURA_PLAYER_HLS_SDK=$(resolveDir $(dirname "$0")/..)
HLS_PLAYER_SDK=$(resolveDir "$1"/HLSPlayerSDK)

cd $(dirname $0)

# Build native code
ndk-build -C $HLS_PLAYER_SDK

# Create lib.jar with the shared objects.
rm -rf temp
mkdir temp
cd temp
mkdir lib
cp -R $HLS_PLAYER_SDK/libs/armeabi $HLS_PLAYER_SDK/libs/armeabi-v7a $HLS_PLAYER_SDK/libs/x86 $HLS_PLAYER_SDK/libs/mips lib
jar cfvM lib.jar lib

# Copy the native part (now in lib.jar).
cp lib.jar $KALTURA_PLAYER_HLS_SDK/libs

# Copy the Java source files.
rm -rf "$KALTURA_PLAYER_HLS_SDK"/src/main/java/com
cp -R "$HLS_PLAYER_SDK"/src/com "$KALTURA_PLAYER_HLS_SDK"/src/main/java


