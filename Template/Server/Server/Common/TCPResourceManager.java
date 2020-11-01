package Server.Common;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class TCPResourceManager extends ResourceManager {

    private static ExecutorService executor = null;
    private static int threads = 10;
    private static TCPResourceManager manager = null;
    private static String s_serverName = "Server";
    private static int s_serverPort = 12345;
    private static final Logger logger = Logger.getLogger(TCPResourceManager.class.getName());

    private ServerSocket serverSocket;

    public TCPResourceManager(String p_name) {
        super(p_name);
    }

    public static void main(String[] args) {
        if(args.length > 0) {
            s_serverName = args[0];
            s_serverPort = Integer.parseInt(args[1]);
        }

        manager = new TCPResourceManager(s_serverName);
        manager.start(s_serverPort);
    }

    public void start(int port) {
        try {
            this.executor = Executors.newFixedThreadPool(threads);
            logger.info("TCPResourceManager " + m_name + " initialized.");
            logger.info("TCPResourceManager " + m_name + " has a thread pool of " + threads + " threads.");
            serverSocket = new ServerSocket(port);
            logger.info("TCPResourceManager " + s_serverName + " binded to port " + port);
            while(true) {
                executor.submit(new Handler(serverSocket.accept(), this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Handler implements Runnable {
        private Socket socket;
        private PrintWriter outToClient;
        private BufferedReader inFromClient;
        private TCPResourceManager manager = null;

        public Handler(Socket socket, TCPResourceManager manager) {
            this.socket = socket;
            this.manager = manager;
            logger.info("Connected by " + this.socket.getRemoteSocketAddress().toString());
        }

        public void run() {
            try {
//                Thread.sleep(10000);
                logger.info("Thread: " + Thread.currentThread().getName());
                outToClient = new PrintWriter(socket.getOutputStream(), true);
                inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input = inFromClient.readLine();
                System.out.println(input);
                Vector<String> command = Parser.parse(input);
                System.out.println(command);
                if(command == null) {
                    outToClient.println("");
                    inFromClient.close();
                    outToClient.close();
                    socket.close();
                } else {
                    String response = manager.execute(command);
                    outToClient.println(response);
                    inFromClient.close();
                    outToClient.close();
                    socket.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String execute(Vector<String> command) {
        try {
            switch (command.get(0).toLowerCase()) {
                case "addflight": {
                    logger.info(command.toString());
                    int xid = Integer.parseInt(command.get(1));
                    int flightNumber = Integer.parseInt(command.get(2));
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addFlight(xid, flightNumber, num, price));
                }
                case "addcars": {
                    logger.info(command.toString());
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addCars(xid, location, num, price));
                }
                case "addrooms": {
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    int num = Integer.parseInt(command.get(3));
                    int price = Integer.parseInt(command.get(4));
                    return Boolean.toString(manager.addRooms(xid, location, num, price));
                }
                case "addcustomer": {
                    int xid = Integer.parseInt(command.get(1));
                    return Integer.toString(manager.newCustomer(xid));
                }
                case "addcustomerid": {
                    int xid = Integer.parseInt(command.get(1));
                    int id = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.newCustomer(xid, id));
                }
                case "deleteflight": {
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.deleteFlight(xid, flightNum));
                }
                case "deletecars": {
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Boolean.toString(manager.deleteCars(xid, location));
                }
                case "deleterooms": {
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Boolean.toString(manager.deleteRooms(xid, location));
                }
                case "deletecustomer": {
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    return Boolean.toString(manager.deleteCustomer(xid, customerID));
                }
                case "queryflight": {
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Integer.toString(manager.queryFlight(xid, flightNum));
                }
                case "querycars": {
                    System.out.println("querycars");
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryCars(xid, location));
                }
                case "queryrooms": {
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryRooms(xid, location));
                }
                case "querycustomer": {
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    return manager.queryCustomerInfo(xid, customerID);
                }
                case "queryflightprice": {
                    int xid = Integer.parseInt(command.get(1));
                    int flightNum = Integer.parseInt(command.get(2));
                    return Integer.toString(manager.queryFlightPrice(xid, flightNum));
                }
                case "querycarsprice": {
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryCarsPrice(xid, location));
                }
                case "queryroomsprice": {
                    int xid = Integer.parseInt(command.get(1));
                    String location = command.get(2);
                    return Integer.toString(manager.queryRoomsPrice(xid, location));
                }
                case "reserveflight": {
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    int flightNum = Integer.parseInt(command.get(3));
                    return Boolean.toString(manager.reserveFlight(xid, customerID, flightNum));
                }
                case "reservecar": {
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String location = command.get(3);
                    return Boolean.toString(manager.reserveCar(xid, customerID, location));
                }
                case "reserveroom": {
                    int xid = Integer.parseInt(command.get(1));
                    int customerID = Integer.parseInt(command.get(2));
                    String location = command.get(3);
                    return Boolean.toString(manager.reserveRoom(xid, customerID, location));
                }
                case "bundle": {

                }
            }
        } catch(Exception e) {
            System.err.println((char)27 + "[31;1mExecution exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
        }
        return "";
    }
}
