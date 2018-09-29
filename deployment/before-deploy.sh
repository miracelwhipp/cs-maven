#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then

 	openssl aes-256-cbc -K $encrypted_513526db5958_key -iv $encrypted_513526db5958_iv -in deployment/codesigning.asc.enc -out deployment/codesigning.asc -d

    gpg2 --fast-import --allow-non-selfsigned-uid deployment/codesigning.asc
fi