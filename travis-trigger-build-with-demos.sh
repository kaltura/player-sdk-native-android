#!/bin/bash

if [ $# != 2 ]; then
  echo Usage:
  echo $0 TRAVIS_TOKEN BRANCH_NAME
  exit 1
fi

TRAVIS_TOKEN=$1
BRANCH_NAME=$2

set -ue
define(){ IFS='\n' read -r -d '' ${1} || true; }


define REQ_BODY <<EOF
{
  "request": {
    "branch": "$BRANCH_NAME",
    "config": {
      "env": {
        "BUILD_DEMOS": "true"
      }
    }
  }
}
EOF

curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Travis-API-Version: 3" \
  -H "Authorization: token $TRAVIS_TOKEN" \
  -d "$REQ_BODY" \
  https://api.travis-ci.org/repo/kaltura%2Fplayer-sdk-native-android/requests

