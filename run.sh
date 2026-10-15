#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p data
javac -d . src/QuizCraftServer.java
java -cp . QuizCraftServer
