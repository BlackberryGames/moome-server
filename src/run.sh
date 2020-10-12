#!/bin/bash
clear
./del.sh
javac Main.java && java Main $1
./del.sh
