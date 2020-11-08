# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

java -Djava.security.policy=java.policy -cp javac -cp ../Server/RMIInterface.jar:../Server/RMHashMap.jar:../Server/RMItem.jar:. Client.RMIClientTest $1 $2
