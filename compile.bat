@set SDKPATH="C:\Program Files\Java\jdk1.6.0_21\bin"
@echo (compile 'cljfe.systray) | java -cp lib\clojure.jar;lib\clojure-contrib-1.2.0.jar;classes;src clojure.main
@cd classes
%SDKPATH%\jar cvf cljfe.jar cljfe\*
copy cljfe.jar ..\lib
cd ..
