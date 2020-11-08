package Server.Middleware;

public class ServerConfig {
    public int port;
    public String host;
    public String name;

    public ServerConfig(String name,String port, String host){
        this.name = name;
        this.host = host;
        this.port = Integer.parseInt(port);
    }

}