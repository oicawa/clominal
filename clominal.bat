@echo off

set BASE_PATH=%~dp0
set LIB_PATH=%BASE_PATH%\lib
set SRC_PATH=%BASE_PATH%\src
set CLS_PATH=%BASE_PATH%\classes
set PLG_PATH=%BASE_PATH%\plugins


java ^
-cp ^
.;^
%PLG_PATH%;^
%SRC_PATH%;^
%CLS_PATH%;^
%LIB_PATH%\rsyntaxtextarea-2.0.6.jar;^
%LIB_PATH%\clojure-1.4.0.jar;^
%LIB_PATH%\clojure-contrib-1.2.0.jar ^
clojure.main -i %SRC_PATH%\clominal\core.clj

