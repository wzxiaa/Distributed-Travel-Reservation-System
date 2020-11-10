package Server.Middleware;

import Server.TransactionManager.*;
import Server.LockManager.*;
import java.util.Vector;
import java.rmi.ConnectException;

import Server.Interface.*;
import java.util.*;
import java.io.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;


import Server.Common.*;


public class Middleware extends ResourceManager {
    public static final String FLIGHT_RM = "Flight";
    public static final String ROOM_RM = "Room";
    public static final String CAR_RM = "Car";
    public static final String CUSTOMER_RM = "Customer";

    // protected static ServerConfig s_flightServer;
    // protected static ServerConfig s_carServer;
    // protected static ServerConfig s_roomServer;

    protected static String flightRM_serverName;
    protected static String carRM_serverName;
    protected static String roomRM_serverName;

    protected static String flightRM_serverHost;
    protected static String carRM_serverHost;
    protected static String roomRM_serverHost;

    protected static int flightRM_serverPort;
    protected static int carRM_serverPort;
    protected static int roomRM_serverPort;

    
    protected IResourceManager flightRM = null;
    protected IResourceManager carRM = null;
    protected IResourceManager roomRM = null;

    protected TransactionManager traxManager;
    protected LockManager lockManager;

    public Middleware(String p_name)
    {
        super(p_name);

        lockManager = new LockManager();
        traxManager = new TransactionManager();
        this.setTransactionManager(traxManager);
    }
   
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group_24";

    public static void main(String[] args) {

        // Args: name,host,port: Flight,localhost,1099
        if (args.length == 3) {
            try {
                String[] flightInfo = args[0].split(",");
                String[] carInfo = args[1].split(",");
                String[] roomInfo = args[2].split(",");

                // s_flightServer = new ServerConfig(s_rmiPrefix + flightInfo[0],flightInfo[1],flightInfo[2]);
                // s_carServer = new ServerConfig(s_rmiPrefix + carInfo[0],carInfo[1],carInfo[2]);
                // s_roomServer = new ServerConfig(s_rmiPrefix + roomInfo[0],roomInfo[1],roomInfo[2]);

                flightRM_serverName=s_rmiPrefix + flightInfo[0];
                flightRM_serverHost=flightInfo[1];
                flightRM_serverPort=Integer.parseInt(flightInfo[2]);

                carRM_serverName=s_rmiPrefix + carInfo[0];
                carRM_serverHost=carInfo[1];
                carRM_serverPort=Integer.parseInt(carInfo[2]);

                roomRM_serverName=s_rmiPrefix + roomInfo[0];
                roomRM_serverHost=roomInfo[1];
                roomRM_serverPort=Integer.parseInt(roomInfo[2]);

            }
            catch(Exception e){
                System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
        else {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mInvalid number of arguments; expected 3 args");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Try to connect to the ResourceManagers and register middleware resource manager
        try {
            Middleware middleware = new Middleware(s_serverName);
            middleware.connectServers();
            middleware.createServerEntry();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mMiddleware exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

    }


    public void connectServers()
    {
        // connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
        // connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
        // connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);

        connectServer("Flight", flightRM_serverHost, flightRM_serverPort, flightRM_serverName);
        connectServer("Car",carRM_serverHost, carRM_serverPort, carRM_serverName);
        connectServer("Room", roomRM_serverHost, roomRM_serverPort, roomRM_serverName);
    }

    public void createServerEntry() {
        // Create the RMI server entry
        try {
            // we don't need to create a Middleware object, since 'this' already is one

            // Dynamically generate the stub (client proxy)
            IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(1099);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(1099);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + s_serverName);
                        System.out.println("'" + s_serverName + "' resource manager unbound");
                    }
                    catch(Exception e) {
                        //System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                        //e.printStackTrace();
                    }
                    System.out.println("'" + s_serverName + "' Shut down");
                }
            });
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }



    public int start() throws RemoteException{
        int xid  = traxManager.start();
        Trace.info("Start transaction " + xid);
        return xid;
    }

    public boolean commit(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        if(!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, "Middleware commit: transaction is not active");
        Transaction t = traxManager.getActiveTransaction(xid);
        RMHashMap m = t.get_TMPdata();
        boolean[] relatedRM = t.getRelatedRMs();

        if (relatedRM[0]){
            synchronized (flightRM.m_data){
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    flightRM.m_data.put(key, m.get(key));
                }
            }
        }
        if (relatedRM[1]){
            synchronized (roomRM.m_data) {
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    roomRM.m_data.put(key, m.get(key));
                }
            }
        }
        if (relatedRM[2]){
            synchronized (carRM.m_data) {
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    carRM.m_data.put(key, m.get(key));
                }
            }
        }

        //if it is customer, we need all resources managers to work
        if (relatedRM[0] && relatedRM[1] && relatedRM[2]) {
            synchronized (m_data) {
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    m_data.put(key, m.get(key));
                }
            }
        }

        traxManager.removeActiveTransaction(xid);
        lockManager.UnlockAll(xid);
        return true;
    }

    public boolean shutdown() throws RemoteException {
        carRM.shutdown();
        roomRM.shutdown();
        flightRM.shutdown();
        new Thread() {
            @Override
            public void run() {
                System.out.print("Middleware: shutting down...");
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                }
                System.out.println("finished");
                System.exit(0);
            }
        }.start();
        return true;
    }

    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        Trace.info("Middleware: addFlight");
        lockData(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid,FLIGHT_RM);
        return flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int xid, String location, int numCars, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        Trace.info("Middleware: addCars");
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid, CAR_RM);
        return carRM.addCars(xid, location, numCars, price);
    }

    public boolean addRooms(int xid, String location, int numRooms, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        Trace.info("Middleware: addRooms");
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid,ROOM_RM);
        return roomRM.addRooms(xid, location, numRooms, price);
    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        Trace.info("Middleware: deleteFlight");
        lockData(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid,FLIGHT_RM);
        return flightRM.deleteFlight(xid, flightNum);
    }

    public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        Trace.info("Middleware: deleteCars");
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid,CAR_RM);
        return carRM.deleteCars(xid, location);
    }

    public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        Trace.info("Middleware: deleteRooms");
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid,ROOM_RM);
        return roomRM.deleteRooms(xid, location);
    }
    
    public int queryFlight(int xid, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middlware: queryFlight");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,FLIGHT_RM);
        return flightRM.queryFlight(xid, flightNumber);
    }

    public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryCars");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CAR_RM);
        return carRM.queryCars(xid, location);
    }

    public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: querCustomer");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CUSTOMER_RM);
        return super.queryCustomerInfo(xid,customerID);
    }

    public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryRooms");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,ROOM_RM);
        return roomRM.queryRooms(xid, location);
    }

    public int queryFlightPrice(int xid, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryFlightPrice");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,FLIGHT_RM);
        return flightRM.queryFlightPrice(xid, flightNumber);
    }

    public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryCarsPrice");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CAR_RM);
        return carRM.queryCarsPrice(xid, location);
    }

    public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryRoomsPrice");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,ROOM_RM);
        return roomRM.queryRoomsPrice(xid, location);
    }

    public int newCustomer(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("Middleware: newCustomer(" + xid + ")");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        lockData(xid, customer.getKey(), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(xid,CUSTOMER_RM);

        writeData(xid, customer.getKey(), customer);
        flightRM.newCustomer(xid, cid);
        roomRM.newCustomer(xid, cid);
        carRM.newCustomer(xid, cid);

        Trace.info("Middleware: newCustomer(" + xid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("Middleware: newCustomer(" + xid + ", " + customerID + ")");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CUSTOMER_RM);

        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer != null){
            Trace.info("Middleware: customer(" + xid + ", " + customerID + ") already exists");
            return false;
        } else {
            lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            customer = new Customer(customerID);
            writeData(xid, customer.getKey(), customer);
            flightRM.newCustomer(xid, customerID);
            carRM.newCustomer(xid, customerID);
            roomRM.newCustomer(xid, customerID);
            Trace.info("Middleware:newCustomer(" + xid + ", " + customerID + ") created");
            return true;
        }
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("Middleware: deleteCustomer(" + xid + ", " + customerID + ")");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CUSTOMER_RM);
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer != null) {
            // First, remove all the reservations related to the customer. Then, remove the customer from DB.
            lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            for (String reservedKey : customer.getReservations().keySet()) {
                lockData(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                String type = reservedKey.split("-")[0];
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                String key = reserveditem.getKey();
                int count = reserveditem.getCount();
                if (type.equals(FLIGHT_RM)) {
                    addResourceManagerUsed(xid,FLIGHT_RM);
                    flightRM.removeReservation(xid, customerID, key, count);
                } else if (type.equals(CAR_RM)) {
                    addResourceManagerUsed(xid,CAR_RM);
                    carRM.removeReservation(xid, customerID, key, count);
                } else if (type.equals(ROOM_RM)) {
                    addResourceManagerUsed(xid,ROOM_RM);
                    roomRM.removeReservation(xid, customerID, key, count);
                } else {
                    Trace.warn("Middleware: deleteCustomer type not recognized");
                }
            }
            removeData(xid, customer.getKey());
            Trace.info("Middleware: customer(" + xid + ", " + customerID + ") deleted");
            return true;
        } else {
            Trace.warn("Middleware: customer(" + xid + ", " + customerID + ") doesn't exist");
            return false;
        }
    }

    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        String key = Flight.getKey(flightNumber);
        Trace.info("Middleware: reserveFlight(" + xid + ", customer=" + customerID + ", " + key + ")" );

        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CUSTOMER_RM);
        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,FLIGHT_RM);

        return flightRM.reserveFlight(xid, customerID, flightNumber);
    }


    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        String key = Car.getKey(location);
        Trace.info("Middleware: reserveCar(" + xid + ", customer=" + customerID + ", " + key + ")" );

        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CUSTOMER_RM);
        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CAR_RM);

        return carRM.reserveCar(xid, customerID, location);
    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        String key = Room.getKey(location);
        Trace.info("Middleware: reserveRoom(" + xid + ", customer=" + customerID + ", " + key + ")" );

        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,CUSTOMER_RM);
        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,ROOM_RM);

        return carRM.reserveRoom(xid, customerID, location);
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called" );
        //checkTransaction(xid);

        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,CUSTOMER_RM);
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        HashMap<String, Integer> countraxManagerap = countFlights(flightNumbers);
        HashMap<Integer, Integer> flightPrice = new HashMap<Integer, Integer>();
        int carPrice;
        int roomPrice;

        if (car && room) {
            // Check flight availability
            for (String key : countraxManagerap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                lockData(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,FLIGHT_RM);
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,CAR_RM);
            carPrice = carRM.itemsAvailable(xid, Car.getKey(location), 1);

            if (carPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                return false;
            }

            lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,ROOM_RM);
            roomPrice = roomRM.itemsAvailable(xid, Room.getKey(location), 1);

            if (roomPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                return false;
            }

            lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            roomRM.reserveRoom(xid, customerID, location);

            lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,CUSTOMER_RM);
            customer.reserve(Room.getKey(location), location, roomPrice);

            writeData(xid, customer.getKey(), customer);

            lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            carRM.reserveCar(xid, customerID, location);

            // Already have customer LOCK_WRITE
            customer.reserve(Car.getKey(location), location, carPrice);
            writeData(xid, customer.getKey(), customer);




        } else if (car) {
            // Check flight availability
            for (String key : countraxManagerap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                lockData(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,FLIGHT_RM);
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,CAR_RM);
            carPrice = carRM.itemsAvailable(xid, Car.getKey(location), 1);

            if (carPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                return false;
            }
            lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            carRM.reserveCar(xid, customerID, location);

            lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,CUSTOMER_RM);
            customer.reserve(Car.getKey(location), location, carPrice);
            writeData(xid, customer.getKey(), customer);


        } else if (room) {
            // Check flight availability
            for (String key : countraxManagerap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                lockData(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,FLIGHT_RM);
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,ROOM_RM);
            roomPrice = roomRM.itemsAvailable(xid, Room.getKey(location), 1);

            if (roomPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                return false;
            }
            lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            roomRM.reserveRoom(xid, customerID, location);

            lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,CUSTOMER_RM);
            customer.reserve(Room.getKey(location), location, roomPrice);
            writeData(xid, customer.getKey(), customer);

        }
        else{
            // Check flight availability
            for (String key : countraxManagerap.keySet()) {
                int keyInt;

                try {
                    keyInt = Integer.parseInt(key);
                } catch (Exception e) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--could not parse flightNumber");
                    return false;
                }
                lockData(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,FLIGHT_RM);
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
        }

        if (flightPrice.keySet().size() > 0) {
            lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,CUSTOMER_RM);
        }
        // Reserve flights
        for (Integer key : flightPrice.keySet()) {
            for (int i = 0; i < countraxManagerap.get(String.valueOf(key)); i++) {
                int price = flightPrice.get(key);

                lockData(xid, Flight.getKey(key), TransactionLockObject.LockType.LOCK_WRITE);
                flightRM.reserveFlight(xid, customerID, key);
                customer.reserve(Flight.getKey(key), String.valueOf(key), price);
                writeData(xid, customer.getKey(), customer);
            }
        }


        Trace.info("RM:bundle() -- succeeded");
        return true;

    }


    public String Summary(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("Middleware: Summary");
        Transaction t = traxManager.getActiveTransaction(xid);
        t.resetTimer();

        RMHashMap m = t.get_TMPdata();
        Set<String> keyset = new HashSet<String>(m.keySet());
        keyset.addAll(m_data.keySet());

        String summary = "";

        for (String key: keyset) {
            String type = key.split("-")[0];
            if (!type.equals(CUSTOMER_RM))
                continue;
            lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(xid,CUSTOMER_RM);
            Customer customer = (Customer)readData(xid, key);
            if (customer != null)
                summary += customer.getSummary();
        }
        return summary;
    }

    public String getName() throws RemoteException {
        return m_name;
    }

    protected HashMap<String, Integer> countFlights(Vector<String> flightNumbers) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        for (String flightNumber : flightNumbers) {
            if (map.containsKey(flightNumber))
                map.put(flightNumber, map.get(flightNumber) + 1);
            else
                map.put(flightNumber, 1);
        }
        return map;
    }

    protected void connectServer(String type, String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);

                    switch(type) {
                        case FLIGHT_RM: {
                            flightRM = (IResourceManager)registry.lookup(name);
                            break;
                        }
                        case CAR_RM: {
                            carRM = (IResourceManager)registry.lookup(name);
                            break;
                        }
                        case ROOM_RM: {
                            roomRM = (IResourceManager)registry.lookup(name);
                            break;
                        }
                    }
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // protected void //checkTransaction(int xid) throws TransactionAbortedException, InvalidTransactionException{
    //     if(traxManager.readActiveData(xid) != null) {
    //         traxManager.readActiveData(xid).updateLastAction();
    //         return;
    //     }
    //     Trace.info("Transaction is not active: throw error");

    //     Boolean v = traxManager.readInactiveData(xid);
    //     if (v == null)
    //         throw new InvalidTransactionException(xid, "MW: The transaction doesn't exist");
    //     else if (v.booleanValue() == true)
    //         throw new InvalidTransactionException(xid, "MW: The transaction has already been committed");
    //     else
    //         throw new TransactionAbortedException(xid, "MW: The transaction has been aborted");
    // }

    public void lockData(int xid, String data, TransactionLockObject.LockType lockType) throws RemoteException, TransactionAbortedException, InvalidTransactionException{
        try {
            if (!lockManager.Lock(xid, data, lockType)) {
                Trace.info("Middleware: cannot lock data: lock(" + xid + ", " + data + ", " + lockType + ")");
                throw new InvalidTransactionException(xid, "LockManager cannot lock data");
            }
        } catch (DeadlockException e) {
            Trace.info("Middleware: deadlock detected: lock(" + xid + ", " + data + ", " + lockType + ") " + e.getLocalizedMessage());
            traxManager.abort(xid);
            throw new TransactionAbortedException(xid, "Deadlock detected: abort transaction");
        }
    }

    public void addResourceManagerUsed(int xid, String resource) throws RemoteException  {
        Transaction t = traxManager.getActiveTransaction(xid);
        t.setRelatedRM(resource);

        try {
            try {

                switch (resource) {
                    case FLIGHT_RM: {
                        flightRM.addTransaction(xid);
                        break;
                    }
                    case CAR_RM: {
                        carRM.addTransaction(xid);
                        break;
                    }
                    case ROOM_RM: {
                        roomRM.addTransaction(xid);
                        break;
                    }
                    case CUSTOMER_RM: {
                        this.addTransaction(xid);
                        flightRM.addTransaction(xid);
                        carRM.addTransaction(xid);
                        roomRM.addTransaction(xid);
                        break;
                    }
                }




            } catch (ConnectException e) {
                switch (resource) {
                    case FLIGHT_RM: {
                        connectServer(FLIGHT_RM, flightRM_serverHost, flightRM_serverPort, flightRM_serverName);
                        flightRM.addTransaction(xid);
                        break;
                    }
                    case CAR_RM: {
                        connectServer(CAR_RM, carRM_serverHost, carRM_serverPort, carRM_serverName);
                        carRM.addTransaction(xid);
                        break;
                    }
                    case ROOM_RM: {
                        connectServer(ROOM_RM, roomRM_serverHost, roomRM_serverPort, roomRM_serverName);
                        roomRM.addTransaction(xid);
                        break;
                    }
                    case CUSTOMER_RM: {
                        this.addTransaction(xid);
                    }
                }
            }
        } catch (Exception e) {
            Trace.error(e.toString());
        }
    }
}