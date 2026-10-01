#!/bin/bash
# Compile-check the app and run its JVM unit tests without Google Maven.
# First run downloads ~230 MB of jars into libs/ (Maven Central only).
set -euo pipefail
cd "$(dirname "$0")"
M=https://repo1.maven.org/maven2
mkdir -p libs
fetch() { # url dest; retries because Maven Central rate-limits bursts
  [ -s "$2" ] && return 0
  for i in 1 2 3 4 5; do curl -sSfL -o "$2" "$1" && return 0; sleep $((i * 4)); done
  echo "Download failed: $1" >&2; exit 1
}
aar_classes() { # aar-url dest-jar
  [ -s "$2" ] && return 0
  fetch "$1" libs/tmp.aar && unzip -o -q libs/tmp.aar classes.jar -d libs/tmp && mv libs/tmp/classes.jar "$2" && rm -rf libs/tmp libs/tmp.aar
}
fetch $M/org/robolectric/android-all/16-robolectric-13921718/android-all-16-robolectric-13921718.jar libs/android-all.jar
aar_classes $M/org/opencv/opencv/4.14.0/opencv-4.14.0.aar libs/opencv.jar
aar_classes $M/org/tensorflow/tensorflow-lite/2.16.1/tensorflow-lite-2.16.1.aar libs/tflite.jar
aar_classes $M/org/tensorflow/tensorflow-lite-api/2.16.1/tensorflow-lite-api-2.16.1.aar libs/tflite-api.jar
gradle compileKotlin test --console=plain "$@"
