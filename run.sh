#!/bin/sh

exec mvn exec:java -Dexec.args="$*"

