#!/bin/bash

if ! command -v java &> /dev/null; then
    echo -e "\033[31mERROR: 'java' not found in PATH.\033[0m"
    echo -e "\033[33mInstall Java 21+ and ensure JAVA_HOME/bin is in your PATH.\033[0m"
    echo -e "\033[33mSee README.md for setup instructions.\033[0m"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$(find "$SCRIPT_DIR/target" -maxdepth 1 -type f -name 'processing-server-*.jar' \
    ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-original.jar' \
    -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)"
CONFIG_PATH="$SCRIPT_DIR/config/application-https.yaml"
KEYSTORE_PATH="$SCRIPT_DIR/keystore.p12"

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo -e "\033[31mERROR: Packaged jar not found under $SCRIPT_DIR/target\033[0m"
    echo -e "\033[33mRun 'mvn package -DskipTests' first.\033[0m"
    exit 1
fi

if [ ! -f "$CONFIG_PATH" ]; then
    echo -e "\033[31mERROR: HTTPS config file not found: $CONFIG_PATH\033[0m"
    exit 1
fi

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo -e "\033[31mERROR: HTTPS keystore not found: $KEYSTORE_PATH\033[0m"
    echo -e "\033[33mRun './create-keystore.sh' first.\033[0m"
    exit 1
fi

echo -e "\033[32mStarting Processing Server...\033[0m"
echo -e "\033[36mServer will be available at:\033[0m"
echo -e "\033[37m  - http://localhost:8080\033[0m"
echo -e "\033[37m  - https://localhost:8443\033[0m"
echo ""
echo -e "\033[90mHTTPS config:  $CONFIG_PATH\033[0m"
echo -e "\033[90mHTTPS keystore: $KEYSTORE_PATH\033[0m"
echo ""

java -Dapp.config="$CONFIG_PATH" -jar "$JAR_PATH"
