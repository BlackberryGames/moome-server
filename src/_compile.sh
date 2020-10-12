#!/bin/sh

cd "/usr/share/moome-server/"

find . -name "*.class" -delete
javac Main.java
