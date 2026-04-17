#!/bin/bash

PROPERTIES="${1:-}"
set -e

if ! command -v java &> /dev/null; then
    echo -e "\033[31mERROR: 'java' not found in PATH.\033[0m"
    echo -e "\033[33mInstall Java 21+ and ensure JAVA_HOME/bin is in your PATH.\033[0m"
    echo -e "\033[33mSee README.md for setup instructions.\033[0m"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "\033[31mERROR: 'mvn' not found in PATH.\033[0m"
    echo -e "\033[33mInstall Maven and ensure it is available before using auto-build launch scripts.\033[0m"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
latest_jar() {
    find "$SCRIPT_DIR/target" -maxdepth 1 -type f -name 'processing-server-*.jar' \
    ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*-original.jar' \
    -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | cut -d' ' -f2-
}

latest_source_epoch() {
    local latest=0
    local path
    for path in "$SCRIPT_DIR/src" "$SCRIPT_DIR/generated-src" "$SCRIPT_DIR/config" "$SCRIPT_DIR/pom.xml"; do
        [ -e "$path" ] || continue
        local candidate=0
        if [ -d "$path" ]; then
            candidate="$(find "$path" -type f -printf '%T@\n' 2>/dev/null | sort -nr | head -n 1)"
        else
            candidate="$(find "$path" -maxdepth 0 -printf '%T@\n' 2>/dev/null | head -n 1)"
        fi
        candidate="${candidate%%.*}"
        [ -n "$candidate" ] || candidate=0
        if [ "$candidate" -gt "$latest" ]; then
            latest="$candidate"
        fi
    done
    echo "$latest"
}

JAR_PATH="$(latest_jar)"
JAR_EPOCH=0
if [ -n "$JAR_PATH" ] && [ -f "$JAR_PATH" ]; then
    JAR_EPOCH="$(find "$JAR_PATH" -maxdepth 0 -printf '%T@\n' 2>/dev/null | head -n 1)"
    JAR_EPOCH="${JAR_EPOCH%%.*}"
fi
SOURCE_EPOCH="$(latest_source_epoch)"

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ] || [ "$SOURCE_EPOCH" -gt "$JAR_EPOCH" ]; then
    echo -e "\033[33mSource changes detected. Building packaged jar...\033[0m"
    mvn -q -DskipTests package
    JAR_PATH="$(latest_jar)"
fi

echo -e "\033[32mStarting Processing Server...\033[0m"
echo -e "\033[36mServer will be available at:\033[0m"
echo -e "\033[37m  - http://localhost:8080\033[0m"
echo ""
echo -e "\033[90mOptional LAN HTTPS is available via ./run-https.sh after generating keystore.p12.\033[0m"
echo ""

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo -e "\033[31mERROR: Packaged jar not found under $SCRIPT_DIR/target\033[0m"
    exit 1
fi

PROPERTY_ARGS=()
if [ -n "$PROPERTIES" ]; then
    read -r -a PROPERTY_ARGS <<< "$PROPERTIES"
fi

java "${PROPERTY_ARGS[@]}" -jar "$JAR_PATH"
