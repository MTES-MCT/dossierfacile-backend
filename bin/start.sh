#!/bin/bash

ln -s /app/.apt/usr/lib/x86_64-linux-gnu/lapack/liblapack.so.3 /app/.apt/usr/lib/x86_64-linux-gnu/
ln -s /app/.apt/usr/lib/x86_64-linux-gnu/blas/libblas.so.3 /app/.apt/usr/lib/x86_64-linux-gnu/

java $JVM_OPTIONS -Djna.library.path=$JNA_LIBRARY_PATH -jar $APP_DIR/target/$APP_DIR.jar
