#!/bin/bash

# Mandatory environment variables:
# $TRAVIS_PULL_REQUEST (number or "false"), $ARTIFACT_UPLOAD_URL (url), $ARTIFACT_UPLOAD_TOKEN (string token)

set -ue


# only pulls
[ "$TRAVIS_PULL_REQUEST" = "false" ] && exit


upload_artifacts() {
    for F in $ARTIFACTS_TO_UPLOAD; do
        if [ -f ${F} ]; then
            curl "$ARTIFACT_UPLOAD_URL" -F token="$ARTIFACT_UPLOAD_TOKEN" \
                -F pull="$TRAVIS_PULL_REQUEST" -F platform="$ARTIFACT_UPLOAD_PLATFORM" -F upfile=@"$F"
        fi
    done
}


# Set variables and upload files

ARTIFACT_UPLOAD_PLATFORM=android
ARTIFACTS_TO_UPLOAD="kalturaPlay/build/outputs/apk/kalturaPlay-debug.apk testapp/build/outputs/apk/testapp-debug.apk"

upload_artifacts
