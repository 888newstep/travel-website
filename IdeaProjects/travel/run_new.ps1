# 编译项目
mvn clean compile

# 生成classpath.txt文件
mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt

# 读取classpath.txt文件的内容
$classpath = Get-Content classpath.txt

# 运行项目
java -cp "target\classes;$classpath" travel.TravelApplication