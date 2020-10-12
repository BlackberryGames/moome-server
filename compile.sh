#!/bin/sh

cd "$(dirname "$0")/src"

find . -name "*.class" -delete
javac Main.java
