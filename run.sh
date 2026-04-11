#!/bin/bash

if ! command -v java &> /dev/null; then
    echo -e "\033[31mERROR: 'java' not found in PATH.\033[0m"
    echo -e "\033[33mInstall Java 21+ and ensure JAVA_HOME/bin is in your PATH.\033[0m"
    echo -e "\033[33mSee README.md for setup instructions.\033[0m"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "\033[32mStarting Processing Server...\033[0m"
echo -e "\033[36mServer will be available at:\033[0m"
echo -e "\033[37m  - https://localhost:8080\033[0m"
echo ""
echo -e "\033[90mNote: Check console output for your local IP\033[0m"
echo ""

java -cp "$SCRIPT_DIR/target/classes:$SCRIPT_DIR/target/libs/*" com.processing.server.Main