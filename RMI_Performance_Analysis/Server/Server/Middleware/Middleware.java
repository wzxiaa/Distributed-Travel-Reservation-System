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

    public Middleware(String p_name) {
        super(p_name);

        lockManager = new LockManager();
        traxManager = new TransactionManager();
        traxManager.setLockManager(lockManager);
        this.setTransactionManager(traxManager);
    }

    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group_24_";

    public static void main(String[] args) {

        // Args: name,host,port: Flight,localhost,1099
        if (args.length == 3) {
            try {
                String[] flightInfo = args[0].split(",");
                String[] carInfo = args[1].split(",");
                String[] roomInfo = args[2].split(",");

                flightRM_serverName = s_rmiPrefix + flightInfo[0];
                flightRM_serverHost = flightInfo[1];
                flightRM_serverPort = Integer.parseInt(flightInfo[2]);

                carRM_serverName = s_rmiPrefix + carInfo[0];
                carRM_serverHost = carInfo[1];
                carRM_serverPort = Integer.parseInt(carInfo[2]);

                roomRM_serverName = s_rmiPrefix + roomInfo[0];
                roomRM_serverHost = roomInfo[1];
                roomRM_serverPort = Integer.parseInt(roomInfo[2]);

            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mInvalid number of arguments; expected 3 args");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Try to connect to the ResourceManagers and register middleware resource manager
        try {
            Middleware middleware = new Middleware(s_serverName);
            middleware.connectServers();
            middleware.createServerEntry();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void connectServers() {
        connectServer("Flight", flightRM_serverHost, flightRM_serverPort, flightRM_serverName);
        connectServer("Car", carRM_serverHost, carRM_serverPort, carRM_serverName);
        connectServer("Room", roomRM_serverHost, roomRM_serverPort, roomRM_serverName);
    }

    public void createServerEntry() {
        // Create the RMI server entry
        try {
            // we don't need to create a Middleware object, since 'this' already is one

            // Dynamically generate the stub (client proxy)
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(12345);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(12345);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + s_serverName);
                        System.out.println("'" + s_serverName + "' resource manager unbound");
                    } catch (Exception e) {
                        //System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                        //e.printStackTrace();
                    }
                    System.out.println("'" + s_serverName + "' Shut down");
                }
            });
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public long[] start() throws RemoteException {
        long time = getCurrentTime();
        int xid = traxManager.start();
        Trace.info("Start transaction " + xid);
        return new long[]{xid, getCurrentTime() - time};
    }

    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        traxManager.abort(xid);
        lockManager.UnlockAll(xid);
    }

    public long[] commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        long start = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, "Middleware commit: transaction is not active");
        Transaction t = traxManager.getActiveTransaction(xid);
        RMHashMap m = t.get_TMPdata();
        boolean[] relatedRM = t.getRelatedRMs();

        long time1 = 0;
        long time2 = 0;
        long time3 = 0;
        long time4 = 0;

        //if it is customer, we need all resources managers to work
        if (relatedRM[0] && relatedRM[1] && relatedRM[2]) {
            synchronized (m_data) {
                for (String key : m.keySet()) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    long t0 = getCurrentTime();
                    m_data.put(key, m.get(key));
                    time1 += (getCurrentTime() - t0);
                }

            }
        }
        if (relatedRM[0]) {
            for (String key : flightRM.getTraxData(xid).keySet()) {
                System.out.println("Write:(" + key + "," + flightRM.getTraxData(xid).get(key) + ")");
                long t0 = getCurrentTime();
                flightRM.putData(key, flightRM.getTraxData(xid).get(key));
                time2 += (getCurrentTime() - t0);
            }
            flightRM.removeTrax(xid);
        }
        if (relatedRM[1]) {
            for (String key : roomRM.getTraxData(xid).keySet()) {
                System.out.println("Write:(" + key + "," + roomRM.getTraxData(xid).get(key) + ")");
                long t0 = getCurrentTime();
                roomRM.putData(key, roomRM.getTraxData(xid).get(key));
                time3 += (getCurrentTime() - t0);
            }
            roomRM.removeTrax(xid);
        }
        if (relatedRM[2]) {
            for (String key : carRM.getTraxData(xid).keySet()) {
                System.out.println("Write:(" + key + "," + carRM.getTraxData(xid).get(key) + ")");
                long t0 = getCurrentTime();
                carRM.putData(key, carRM.getTraxData(xid).get(key));
                time4 += (getCurrentTime() - t0);
            }
            carRM.removeTrax(xid);
        }
        traxManager.removeActiveTransaction(xid);

        lockManager.UnlockAll(xid);
        long mdw_time = getCurrentTime() - start;
        return new long[]{mdw_time, (time1 + time2 + time3 + time4)};
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

    public long[] addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: addFlight");

        long time0 = getCurrentTime();

        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, FLIGHT_RM);

        long time1 = getCurrentTime();
        long r[] = flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
        return new long[]{time1 - time0, r[0], r[1]};
    }

    public long[] addCars(int xid, String location, int numCars, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: addCars");
        long time0 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, CAR_RM);

        long time1 = getCurrentTime();
        long r[] = carRM.addCars(xid, location, numCars, price);

        long time2 = getCurrentTime() - time1;
        return new long[]{time1 - time0, r[0], r[1]};
    }

    public long[] addRooms(int xid, String location, int numRooms, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: addRooms");
        long time0 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, ROOM_RM);
        long time1 = getCurrentTime();
        long r[] = roomRM.addRooms(xid, location, numRooms, price);
        long time2 = getCurrentTime() - time1;
        return new long[]{time1 - time0, r[0], r[1]};
    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: deleteFlight");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, FLIGHT_RM);
        return flightRM.deleteFlight(xid, flightNum);
    }

    public boolean deleteCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: deleteCars");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, CAR_RM);
        return carRM.deleteCars(xid, location);
    }

    public boolean deleteRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: deleteRooms");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, ROOM_RM);
        return roomRM.deleteRooms(xid, location);
    }

    //    public int queryFlight(int xid, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
//        Trace.info("Middlware: queryFlight");
//        if (!traxManager.isActive(xid))
//            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
//        Transaction trx = traxManager.getActiveTransaction(xid);
//        trx.resetTimer();
//        lockData(xid, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, FLIGHT_RM);
//        return flightRM.queryFlight(xid, flightNumber);
//    }
    public long[] queryFlight(int xid, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middlware: queryFlight");
        long time1 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, FLIGHT_RM);

        long time2 = getCurrentTime();
        System.out.println("time2 " + time2);
        long arr[] = flightRM.queryFlight(xid, flightNumber);
        long time3 = getCurrentTime() - time2;
        return new long[]{time2 - time1, arr[0], arr[1], arr[2]};
    }

    //    public int queryCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
//        Trace.info("Middleware: queryCars");
//        if (!traxManager.isActive(xid))
//            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
//        Transaction trx = traxManager.getActiveTransaction(xid);
//        trx.resetTimer();
//        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, CAR_RM);
//        return carRM.queryCars(xid, location);
//    }
    public long[] queryCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryCars");
        long time1 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CAR_RM);
        long time2 = getCurrentTime();
        System.out.println("time2 " + time2);
        long arr[] = carRM.queryCars(xid, location);
        long time3 = getCurrentTime() - time2;
        //return MDWtime, RMtime+DBtime, DBtime
        return new long[]{time2 - time1, arr[0], arr[1], arr[2]};
    }

    public String queryCustomerInfo(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Trace.info("Middleware: querCustomer");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CUSTOMER_RM);
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.info("Middleware: customer(" + xid + ", " + customerID + ") doesn't exist");
            return "customer(" + xid + ", " + customerID + ") doesn't exist\n";
        }
        return flightRM.queryCustomerInfo(xid, customerID) + carRM.queryCustomerInfo(xid, customerID).split("\n", 2)[1] + roomRM.queryCustomerInfo(xid, customerID).split("\n", 2)[1];
    }

    //    public int queryRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
//        if (!traxManager.isActive(xid))
//            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
//        Trace.info("Middleware: queryRooms");
//        Transaction trx = traxManager.getActiveTransaction(xid);
//        trx.resetTimer();
//        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, ROOM_RM);
//        return roomRM.queryRooms(xid, location);
//    }
    public long[] queryRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryRooms");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        long time1 = getCurrentTime();
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, ROOM_RM);

        long time2 = getCurrentTime();
        System.out.println("time2 " + time2);
        long arr[] = roomRM.queryRooms(xid, location);
        long time3 = getCurrentTime() - time2;
        return new long[]{time2 - time1, arr[0], arr[1], arr[2]};
    }

    public int queryFlightPrice(int xid, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Trace.info("Middleware: queryFlightPrice");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, FLIGHT_RM);
        return flightRM.queryFlightPrice(xid, flightNumber);
    }

    public int queryCarsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryCarsPrice");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CAR_RM);
        return carRM.queryCarsPrice(xid, location);
    }

    public int queryRoomsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: queryRoomsPrice");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, ROOM_RM);
        return roomRM.queryRoomsPrice(xid, location);
    }

    public int newCustomer(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: newCustomer(" + xid + ")");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        lockData(xid, customer.getKey(), TransactionLockObject.LockType.LOCK_WRITE);
        forwardTraxToRM(xid, CUSTOMER_RM);

        writeData(xid, customer.getKey(), customer);
        flightRM.newCustomer(xid, cid);
        roomRM.newCustomer(xid, cid);
        carRM.newCustomer(xid, cid);

        Trace.info("Middleware: newCustomer(" + xid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: newCustomer(" + xid + ", " + customerID + ")");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CUSTOMER_RM);

        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer != null) {
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

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: deleteCustomer(" + xid + ", " + customerID + ")");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CUSTOMER_RM);
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
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
                    forwardTraxToRM(xid, FLIGHT_RM);
                    flightRM.removeReservation(xid, customerID, key, count);
                } else if (type.equals(CAR_RM)) {
                    forwardTraxToRM(xid, CAR_RM);
                    carRM.removeReservation(xid, customerID, key, count);
                } else if (type.equals(ROOM_RM)) {
                    forwardTraxToRM(xid, ROOM_RM);
                    roomRM.removeReservation(xid, customerID, key, count);
                } else {
                    Trace.warn("Middleware: deleteCustomer type not recognized");
                }
            }
            removeData(xid, customer.getKey());
            return flightRM.deleteCustomer(xid, customerID) && roomRM.deleteCustomer(xid, customerID) && carRM.deleteCustomer(xid, customerID);
        } else {
            Trace.warn("Middleware: customer(" + xid + ", " + customerID + ") doesn't exist");
            return false;
        }
    }

    //    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
//        Trace.info("Middleware: reserveFlight(" + xid + ", customer=" + customerID + ", " + flightNumber + ")");
//        if (!traxManager.isActive(xid))
//            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
//        Transaction trx = traxManager.getActiveTransaction(xid);
//        trx.resetTimer();
//        String key = Flight.getKey(flightNumber);
//        Trace.info("Middleware: reserve fight for (" + xid + ", customer=" + customerID + ", " + key + ")");
//        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, CUSTOMER_RM);
//        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, FLIGHT_RM);
//        return flightRM.reserveFlight(xid, customerID, flightNumber);
//    }
    public long[] reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: reserveFlight(" + xid + ", customer=" + customerID + ", " + flightNumber + ")");
        long time0 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        String key = Flight.getKey(flightNumber);
        Trace.info("Middleware: reserve fight for (" + xid + ", customer=" + customerID + ", " + key + ")");
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CUSTOMER_RM);
        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, FLIGHT_RM);
        long time1 = getCurrentTime() - time0;

        long r[] = flightRM.reserveFlight(xid, customerID, flightNumber);
        long time2 = getCurrentTime() - time1;
        Trace.info("mdw time " + time1);
        return new long[]{time1, r[0], r[1]};
    }


    //    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
//        Trace.info("Middleware: reserveCar(" + xid + ", customer=" + customerID + ", " + location + ")");
//        if (!traxManager.isActive(xid))
//            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
//        Transaction trx = traxManager.getActiveTransaction(xid);
//        trx.resetTimer();
//        String key = Car.getKey(location);
//        Trace.info("Middleware: reserve car for (" + xid + ", customer=" + customerID + ", " + key + ")");
//        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, CUSTOMER_RM);
//        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, CAR_RM);
//
//        return carRM.reserveCar(xid, customerID, location);
//    }
    public long[] reserveCar(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: reserveCar(" + xid + ", customer=" + customerID + ", " + location + ")");
        long time0 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        String key = Car.getKey(location);
        Trace.info("Middleware: reserve car for (" + xid + ", customer=" + customerID + ", " + key + ")");
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CUSTOMER_RM);
        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CAR_RM);

        long time1 = getCurrentTime() - time0;

        long r[] = carRM.reserveCar(xid, customerID, location);

        Trace.info("mdw time " + time1);

        return new long[]{time1, r[0], r[1]};
    }

//    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
//        Trace.info("Middleware: reserveRoom(" + xid + ", customer=" + customerID + ", " + location + ")");
//        if (!traxManager.isActive(xid))
//            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
//        Transaction trx = traxManager.getActiveTransaction(xid);
//        trx.resetTimer();
//        String key = Room.getKey(location);
//        Trace.info("Middleware: reserve room for (" + xid + ", customer=" + customerID + ", " + key + ")");
//        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, CUSTOMER_RM);
//        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
//        forwardTraxToRM(xid, ROOM_RM);
//
//        return roomRM.reserveRoom(xid, customerID, location);
//    }

    public long[] reserveRoom(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("Middleware: reserveRoom(" + xid + ", customer=" + customerID + ", " + location + ")");
        long time0 = getCurrentTime();
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();
        String key = Room.getKey(location);
        Trace.info("Middleware: reserve room for (" + xid + ", customer=" + customerID + ", " + key + ")");
        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, CUSTOMER_RM);
        lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(xid, ROOM_RM);

        long time1 = getCurrentTime() - time0;

        Trace.info("mdw time " + time1);

        long r[] = roomRM.reserveRoom(xid, customerID, location);
        long time2 = getCurrentTime() - time1;

        return new long[]{time1, r[0], r[1]};
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        int id = xid;
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called");
        //checkTransaction(xid);

        lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        forwardTraxToRM(id, CUSTOMER_RM);
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        ArrayList<Integer> flights = new ArrayList<Integer>();
        ArrayList<Integer> flightprices = new ArrayList<>();

        for (String flight : flightNumbers) {
            flights.add(Integer.parseInt(flight));
        }

        // Firstly check the availability for all the resources required
        // give locks based on the resources first

        // check availability for flights
        for (Integer flight : flights) {
            lockData(xid, Flight.getKey(flight), TransactionLockObject.LockType.LOCK_READ);
            forwardTraxToRM(id, FLIGHT_RM);
            int flight_availability = flightRM.countAvailaleItems(xid, Flight.getKey(flight), 1);
            if (flight_availability <= 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flight.toString() + ", " + location + ") falied--no available items left");
                return false;
            }
            flightprices.add(flightRM.getPrice(xid, Flight.getKey(flight)));
        }

        // check availability for cars
        if (car) {
            lockData(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            forwardTraxToRM(id, CAR_RM);
            int car_availabilty = carRM.countAvailaleItems(xid, Car.getKey(location), 1);
            if (car_availabilty <= 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + Car.getKey(location) + ", " + location + ") falied--no available items left");
                return false;
            }
        }

        // check availability for rooms
        if (room) {
            lockData(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            forwardTraxToRM(id, ROOM_RM);
            int room_availabilty = roomRM.countAvailaleItems(xid, Room.getKey(location), 1);
            if (room_availabilty <= 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + Room.getKey(location) + ", " + location + ") falied--no available items left");
                return false;
            }
        }
        // availability passed
        // add customer availability and write
        for (int i = 0; i < flights.size(); i++) {
            customer.reserve(Flight.getKey(flights.get(i)), String.valueOf(flights.get(i)), flightprices.get(i));
            flightRM.reserveFlight(xid, customerID, flights.get(i));
        }
        // writeData(xid, customer.getKey(customerID), customer);

        if (car) {
            int price = carRM.getPrice(xid, Car.getKey(location));
            customer.reserve(Car.getKey(location), location, price);
            carRM.reserveCar(xid, customerID, location);
        }
        if (room) {
            int price = roomRM.getPrice(xid, Room.getKey(location));
            customer.reserve(Room.getKey(location), location, price);
            roomRM.reserveRoom(xid, customerID, location);
        }

        writeData(xid, customer.getKey(customerID), customer);
        return true;
    }

    // public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException {

    //     if(!traxManager.isActive(xid))
    //         throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
    //     Trace.info("Middleware: querCustomer");
    //     Transaction trx = traxManager.getActiveTransaction(xid);
    //     trx.resetTimer();
    //     lockData(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
    //     forwardTraxToRM(xid,CUSTOMER_RM);
    //     Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
    //     if(customer == null){
    //         Trace.info("Middleware: customer(" + xid + ", " + customerID + ") doesn't exist");
    //         return "customer(" + xid + ", " + customerID + ") doesn't exist\n";
    //     }
    //     return flightRM.queryCustomerInfo(xid,customerID) + carRM.queryCustomerInfo(xid,customerID).split("\n", 2)[1] + roomRM.queryCustomerInfo(xid,customerID).split("\n", 2)[1];
    // }

    public String Summary(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {

        Trace.info("Middleware: Summary");
        if (!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, " Middleware: Not a valid transaction");
        Transaction t = traxManager.getActiveTransaction(xid);
        RMHashMap m = t.get_TMPdata();
        t.resetTimer();
        String s = "";
        Set<String> keyset = new HashSet<String>(m.keySet());
        keyset.addAll(m_data.keySet());
        Customer customer;
        System.out.println("size" + keyset.size());
        for (String key : keyset) {
            String rmType = key.split("-")[0];
            int customerID = Integer.parseInt(key.split("-")[1]);
            System.out.println("rmType" + key.toString());
            if (!rmType.equals(CUSTOMER_RM)) {
                System.out.println("stopped ");
                continue;
            }

            lockData(xid, key, TransactionLockObject.LockType.LOCK_READ);
            forwardTraxToRM(xid, CUSTOMER_RM);
            customer = (Customer) readData(xid, key);

            if (customer != null) {
                s = s.concat("Customer: " + customerID + "\n");
                s = s.concat(flightRM.queryCustomerInfo(xid, customerID)).concat(carRM.queryCustomerInfo(xid, customerID).split("\n", 2)[1]).concat(roomRM.queryCustomerInfo(xid, customerID).split("\n", 2)[1]);
                s = s + customer.getSummary();
            }
        }
        return s;
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

    protected void connectServer(String type, String server, int port, String name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);

                    switch (type) {
                        case FLIGHT_RM: {
                            flightRM = (IResourceManager) registry.lookup(name);
                            break;
                        }
                        case CAR_RM: {
                            carRM = (IResourceManager) registry.lookup(name);
                            break;
                        }
                        case ROOM_RM: {
                            roomRM = (IResourceManager) registry.lookup(name);
                            break;
                        }
                    }
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + name + "]");
                    break;
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + name + "]");
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

    public void lockData(int xid, String data, TransactionLockObject.LockType lockType) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        try {
            if (!lockManager.Lock(xid, data, lockType)) {
                Trace.info("Middleware: cannot lock data: lock(" + xid + ", " + data + ", " + lockType + ")");
                throw new InvalidTransactionException(xid, "LockManager cannot lock data");
            }
        } catch (DeadlockException e) {
            Trace.info("Middleware: deadlock detected: lock(" + xid + ", " + data + ", " + lockType + ") " + e.getLocalizedMessage());
            traxManager.abort(xid);
            lockManager.UnlockAll(xid);
            throw new TransactionAbortedException(xid, "Deadlock detected: abort transaction");
        }
    }

    public void forwardTraxToRM(int xid, String resource) throws RemoteException  {
        Transaction t = traxManager.getActiveTransaction(xid);
        t.setRelatedRM(resource);
        try {
            try {
                switch (resource) {

                    case CAR_RM: {
                        carRM.addNewTrax(xid);
                        break;
                    }
                    case FLIGHT_RM: {
                        flightRM.addNewTrax(xid);
                        break;
                    }
                    case ROOM_RM: {
                        roomRM.addNewTrax(xid);
                        break;
                    }
                    case CUSTOMER_RM: {
                        this.addNewTrax(xid);
                        flightRM.addNewTrax(xid);
                        carRM.addNewTrax(xid);
                        roomRM.addNewTrax(xid);
                        break;
                    }
                }
            } catch (ConnectException e) {
                switch (resource) {
                    case CAR_RM: {
                        connectServer(CAR_RM, carRM_serverHost, carRM_serverPort, carRM_serverName);
                        carRM.addNewTrax(xid);
                        break;
                    }

                    case FLIGHT_RM: {
                        connectServer(FLIGHT_RM, flightRM_serverHost, flightRM_serverPort, flightRM_serverName);
                        flightRM.addNewTrax(xid);
                        break;
                    }

                    case ROOM_RM: {
                        connectServer(ROOM_RM, roomRM_serverHost, roomRM_serverPort, roomRM_serverName);
                        roomRM.addNewTrax(xid);
                        break;
                    }
                    case CUSTOMER_RM: {
                        this.addNewTrax(xid);
                    }
                }
            }
        } catch (Exception e) {
            Trace.error(e.toString());
        }
    }
}
