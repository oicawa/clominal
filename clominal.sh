#!/bin/sh

BASE_PATH=${0%/*}
LIB_PATH=${BASE_PATH}/lib
SRC_PATH=${BASE_PATH}/src
CLS_PATH=${BASE_PATH}/classes
#CFG_PATH=${HOME}/.clominal
CFG_PATH=${BASE_PATH}/plugins

java \
-cp \
.:\
${CFG_PATH}:\
${SRC_PATH}:\
${CLS_PATH}:\
${LIB_PATH}/clojure-1.4.0.jar:\
${LIB_PATH}/clojure-contrib-1.2.0.jar:\
${LIB_PATH}/rsyntaxtextarea-2.0.6.jar \
clojure.main -i ${SRC_PATH}/clominal/core.clj

echo "Finished..."
