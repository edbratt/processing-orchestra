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

echo -e "\033[32mStarting Processing Server...\033[0m"
echo -e "\033[36mServer will be available at:\033[0m"
echo -e "\033[37m  - http://localhost:8080\033[0m"
echo ""
echo -e "\033[90mOptional LAN HTTPS is available via ./run-https.sh after generating keystore.p12.\033[0m"
echo ""

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo -e "\033[31mERROR: Packaged jar not found under $SCRIPT_DIR/target\033[0m"
    echo -e "\033[33mRun 'mvn package -DskipTests' first.\033[0m"
    exit 1
fi

java -jar "$JAR_PATH"
