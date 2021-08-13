IDE = ../1.8.5/LINUX64
CLASSPATH = .:$(IDE)/pde.jar:$(IDE)/arduino-core.jar

T4Security.jar: T4Security.java Frame.java
	javac -target 1.8 -classpath $(CLASSPATH) -d . $^
	zip -9 -q $@ -r cc

clean:
	rm -rf T4Security.jar cc
