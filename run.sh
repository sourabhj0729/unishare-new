#!/bin/sh
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out src/unishare/*.java || exit 1
java -cp "out:lib/*" unishare.Main
