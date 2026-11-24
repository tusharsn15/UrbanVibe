#!/bin/bash
# Compiles UrbanVibe without Maven. Needs the MySQL JDBC jar inside lib/
set -e
if ! ls lib/*.jar >/dev/null 2>&1; then
  echo "Put mysql-connector-j-8.x.jar in the lib/ folder first (see README)."; exit 1
fi
rm -rf out && mkdir out
javac --release 17 -d out -cp "lib/*" $(find src -name "*.java")
echo "Build OK -> out/"
