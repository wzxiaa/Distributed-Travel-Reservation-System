package Client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TCPClient extends Client{

    private static String s_serverHost = "localhost";
    private static int s_serverPort = 3024;
    private static String s_serverName = "Server";

    // TODO: ADD YOUR GROUP NUMBER TO COMPILE
    private static String s_rmiPrefix = "group_24_";

    Socket socket;
    PrintWriter outToServer;
    BufferedReader inFromServer;
    // SocketClient client;

    public TCPClient() {
        super();
    }

    public static void main(String args[]) throws IOException {
        if (args.length > 0) {
            s_serverHost = args[0];
        }
        if (args.length > 1) {
            s_serverName = args[1];
        }
        if (args.length > 2) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
                    + "[0mUsage: java client.SocketClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
//        if (System.getSecurityManager() == null) {
////            System.setSecurityManager(new SecurityManager());
////        }

        try {
            TCPClient client = new TCPClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // String serverName=args[0];

        // Socket socket= new Socket(serverName, 9090); // establish a socket with a
        // server using the given port#

        // PrintWriter outToServer= new PrintWriter(socket.getOutputStream(),true); //
        // open an output stream to the server...
        // BufferedReader inFromServer = new BufferedReader(new
        // InputStreamReader(socket.getInputStream())); // open an input stream from the
        // server...

        // BufferedReader bufferedReader =new java.io.BufferedReader(new
        // InputStreamReader(System.in)); //to read user's input

        // while(true) // works forever
        // {
        // String readerInput=bufferedReader.readLine(); // read user's input
        // if(readerInput.equals("quit"))
        // break;

        // outToServer.println(readerInput); // send the user's input via the output
        // stream to the server
        // String res=inFromServer.readLine(); // receive the server's result via the
        // input stream from the server
        // System.out.println("result: "+res); // print the server result to the user
        // }

        // socket.close();
    }

    @Override
    public void execute(Command cmd, Vector<String> arguments) throws RemoteException, NumberFormatException {

        try {
            switch (cmd) {
                case Help: {
                    if (arguments.size() == 1) {
                        System.out.println(Command.description());
                    } else if (arguments.size() == 2) {
                        Command l_cmd = Command.fromString((String) arguments.elementAt(1));
                        System.out.println(l_cmd.toString());
                    } else {
                        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27
                                + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
                    }
                    break;
                }
                case AddFlight: {
                    checkArgumentsCount(5, arguments.size());

                    System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));
                    System.out.println("-Flight Seats: " + arguments.elementAt(3));
                    System.out.println("-Flight Price: " + arguments.elementAt(4));

                    process(arguments.toString(), "Flight added", "Flight could not be added", "BOOL");
                    break;
                }
                case AddCars: {
                    checkArgumentsCount(5, arguments.size());

                    System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));
                    System.out.println("-Number of Cars: " + arguments.elementAt(3));
                    System.out.println("-Car Price: " + arguments.elementAt(4));

                    process(arguments.toString(), "Cars added", "Cars could not be added", "BOOL");
                    break;
                }
                case AddRooms: {
                    checkArgumentsCount(5, arguments.size());

                    System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Room Location: " + arguments.elementAt(2));
                    System.out.println("-Number of Rooms: " + arguments.elementAt(3));
                    System.out.println("-Room Price: " + arguments.elementAt(4));

                    process(arguments.toString(), "Rooms added", "Rooms could not be added", "BOOL");
                    break;
                }
                case AddCustomer: {
                    // TODO: handle this case
                    checkArgumentsCount(2, arguments.size());

                    System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

                    process(arguments.toString(), "Customer added", "Customer could not be added", "INT");
                    break;
                }
                case AddCustomerID: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));

                    process(arguments.toString(), "Customer added", "Customer could not be added", "BOOL");
                    break;
                }
                case DeleteFlight: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));

                    process(arguments.toString(), "Flight Deleted", "Flight could not be deleted", "BOOL");
                    break;
                }
                case DeleteCars: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));

                    process(arguments.toString(), "Cars Deleted", "Cars could not be deleted", "BOOL");
                    break;
                }
                case DeleteRooms: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));

                    process(arguments.toString(), "Rooms Deleted", "Rooms could not be deleted", "BOOL");
                    break;
                }
                case DeleteCustomer: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));

                    process(arguments.toString(), "Customer Deleted", "Customer could not be deleted", "BOOL");
                    break;
                }
                case QueryFlight: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));

                    process(arguments.toString(), "Number of seat queried", "Could not query number of seats", "INT");
                    break;
                }
                case QueryCars: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));

                    process(arguments.toString(), "Number of cars at this location queried",
                            "Could not query number of cars", "INT");
                    break;
                }
                case QueryRooms: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Room Location: " + arguments.elementAt(2));

                    process(arguments.toString(), "Number of rooms at this location queried",
                            "Could not query number of rooms", "INT");
                    break;
                }
                case QueryCustomer: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));

                    process(arguments.toString(), "Billed for the customer queried", "Could not query customer", "STR");
                    break;
                }
                case QueryFlightPrice: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Flight Number: " + arguments.elementAt(2));

                    process(arguments.toString(), "Price of a seat queried", "Could not query price of seat", "INT");
                    break;
                }
                case QueryCarsPrice: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Car Location: " + arguments.elementAt(2));

                    process(arguments.toString(), "Price of cars at this location queried", "Could not query price of cars",
                            "INT");
                    break;
                }
                case QueryRoomsPrice: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Room Location: " + arguments.elementAt(2));

                    process(arguments.toString(), "Price of rooms at this location queried",
                            "Could not query price of rooms", "INT");
                    break;
                }
                case ReserveFlight: {
                    checkArgumentsCount(4, arguments.size());

                    System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    System.out.println("-Flight Number: " + arguments.elementAt(3));

                    process(arguments.toString(), "Flight Reserved", "Flight could not be reserved", "BOOL");
                    break;
                }
                case ReserveCar: {
                    checkArgumentsCount(4, arguments.size());

                    System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    System.out.println("-Car Location: " + arguments.elementAt(3));

                    process(arguments.toString(), "Car Reserved", "Car could not be reserved", "BOOL");
                    break;
                }
                case ReserveRoom: {
                    checkArgumentsCount(4, arguments.size());

                    System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    System.out.println("-Room Location: " + arguments.elementAt(3));

                    process(arguments.toString(), "Room Reserved", "Room could not be reserved", "BOOL");
                    break;
                }
                case Bundle: {
                    // TODO: implement bundle
                    if (arguments.size() < 7) {
                    System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 +
                    "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
                    break;
                    }

                    System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) +
                    "]");
                    System.out.println("-Customer ID: " + arguments.elementAt(2));
                    for (int i = 0; i < arguments.size() - 6; ++i)
                    {
                    System.out.println("-Flight Number: " + arguments.elementAt(3+i));
                    }
                    System.out.println("-Location for Car/Room: " +
                    arguments.elementAt(arguments.size()-3));
                    System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
                    System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    Vector<String> flightNumbers = new Vector<String>();
                    for (int i = 0; i < arguments.size() - 6; ++i)
                    {
                    flightNumbers.addElement(arguments.elementAt(3+i));
                    }
                    String location = arguments.elementAt(arguments.size()-3);
                    boolean car = toBoolean(arguments.elementAt(arguments.size()-2));
                    boolean room = toBoolean(arguments.elementAt(arguments.size()-1));

                    if (m_resourceManager.bundle(id, customerID, flightNumbers, location, car,
                    room)) {
                    System.out.println("Bundle Reserved");
                    } else {
                    System.out.println("Bundle could not be reserved");
                    }
                    break;
                }
                case Quit:
                    checkArgumentsCount(1, arguments.size());
                    stopClient();
                    System.out.println("Quitting client");
                    System.exit(0);
            }
        } catch (IOException e) {
            connectServer();
            execute(cmd, arguments);
        }
    }

    public void process(String input, String successMessage, String failureMessage, String returnType)
            throws IOException {
        // send the user's input via the output stream to the server
        // receive the server's result via the input stream from the server
        // establish a new connection before finishing
        outToServer.println(input);
        String line = inFromServer.readLine();
        String res = "";
        res = line;
//        while (line != null) {
//            System.out.println(line);
//            if (res.length() == 0)
//                res = line;
//            else
//                res = "\n" + line;
//        }
        try{
             connectServer();
        }catch(Exception e){
          e.printStackTrace();
        }
        System.out.println("result: " + res); // print the server result to the user
        boolean success = false;
        try {
            if (res.length() > 0) {
                if (returnType.equals("BOOL")) {
                    if (toBoolean(res)) {
                        success = true;
                        System.out.println(successMessage);
                    }
                } else if (returnType.equals("INT")) {
                    success = true;
                    System.out.println(successMessage + toInt(res));
                } else if (returnType.equals("STR")) {
                    success = true;
                    System.out.println(successMessage + res);
                }
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
	    e.printStackTrace();
        }
        if (!success)
            System.out.println(failureMessage);
    }

    @Override
    public void connectServer() {
        try {
            boolean first = true;
            while (true) {
                try {
                    socket = new Socket(s_serverHost, s_serverPort);
                    outToServer = new PrintWriter(socket.getOutputStream(), true);
                    inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream())); // open an input
                    // stream from
                    // the server...
                    System.out.println("Connecting to host:" + s_serverHost + ", port:" + s_serverPort);
                    break;
                } catch (IOException e) {
                    if (first) {
                        System.out.println("Waiting for host:" + s_serverHost + ", port:" + s_serverPort);
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stopClient() {
        try {
            inFromServer.close();
            outToServer.close();
            socket.close();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: cannot disconnect" + (char) 27
                    + "[0mUncaught exception: " + e.toString());
        }
    }
}
