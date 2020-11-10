all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java 
	make -C ../Server/ RMIInterface.jar

../Server/RMItem.jar: ../Server/Server/Common/RMItem.java
	@echo "make -C RMItem"
	make -C ../Server/RMItem.jar

../Server/RMHashMap.jar: ../Server/Server/Common/RMHashMap.java
	@echo "make -C RMHashMap"
	make -C ../Server/RMHashMap.jar

compile-client: ../Server/RMIInterface.jar ../Server/RMHashMap.jar ../Server/RMItem.jar
	javac -cp ../Server/RMIInterface.jar:../Server/RMHashMap.jar:../Server/RMItem.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy