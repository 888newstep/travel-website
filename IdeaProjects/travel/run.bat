@echo off
setlocal

rem 编译项目
mvn clean compile

rem 生成classpath.txt文件
mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt

rem 读取classpath.txt文件的内容
set /p CLASSPATH=<classpath.txt

rem 运行项目
java -cp "target/classes;%CLASSPATH%" travel.TravelApplication

endlocal
pause