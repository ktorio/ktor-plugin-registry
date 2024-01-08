#!/bin/zsh
SPACE_PACKAGES_PUBLISH_TOKEN=$(cat space_token)
curl -i \
  -H "Authorization: Bearer $SPACE_PACKAGES_PUBLISH_TOKEN" \
  https://packages.jetbrains.team/files/p/ktor/files/plugin-registry/ \
  --upload-file build/distributions/registry.tar.gz