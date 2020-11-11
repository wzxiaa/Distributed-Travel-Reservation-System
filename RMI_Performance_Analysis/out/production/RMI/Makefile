all: java.policy compile-server-rmi

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/*.java
	jar cvf RMIInterface.jar Server/Interface/*.class

RMHashMap.jar: Server/Common/RMHashMap.java
	@echo "Compiling RMHashMap jar"
	javac Server/Common/RMHashMap.java
	jar cvf RMHashMap.jar Server/Common/RMHashMap.class

RMItem.jar: Server/Common/RMItem.java
	@echo "Compiling RMItem jar"
	javac Server/Common/RMItem.java
	jar cvf RMItem.jar Server/Common/RMItem.class

compile-server-rmi: Server/Interface/IResourceManager.java Server/Common/*.java Server/Middleware/*.java
	javac Server/Interface/IResourceManager.java Server/Common/*.java Server/Middleware/*.java

clean:
	rm -f RMIInterface.jar
	rm -f RMHashMap.jar
	rm -f RMItem.jar	
	rm -f java.policy 
