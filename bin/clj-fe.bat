@echo off
set L=%~dp0..\lib
set I=%~dp0..\images
java -cp "%L%\clojure.jar;%L%\clojure-contrib-1.2.0.jar;%L%\cljfe.jar" cljfe.systray "%I%"
