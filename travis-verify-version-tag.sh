#!/bin/bash

die() {
  >&2 echo "error: version name does not match tag name"
  exit 1
}

# Only check tags
if [ -z "$TRAVIS_TAG" ]; then exit; fi

# Tags are named "v1.2.3" -- remove the "v" to match versionName.
TAG_VERSION_NAME=${TRAVIS_TAG:1}

# Check that global version matches the tag.
grep -q "version: '$TAG_VERSION_NAME'" kalturaCommon.gradle || die
