#!/bin/bash

# only pulls
[ "$TRAVIS_PULL_REQUEST" = "false" ] && exit

die() {
  >&2 echo "error: hacks found in code"
  exit 1
}

git grep "#HACK#" -- 'playerSDK/*' 'kalturaPlay/*' 'googlemediaframework/*' && die
