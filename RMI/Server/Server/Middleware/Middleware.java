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
import java.rmi.RemoteException;
import java.rmi.NotBoundException;


import Server.Common.*;


public class Middleware extends ResourceManager {

    protected static ServerConfig s_flightServer;
    protected static ServerConfig s_carServer;
    protected static ServerConfig s_roomServer;

    
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

    public int start() throws RemoteException{
        int xid  = traxManager.start();
        Trace.info("Start transaction .... " + xid);
        return xid;
    }

    public boolean commit(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        //int id = xid;
      //  System.out.print("Commit transaction:" + xid);

        //checkTransaction(xid);
        Transaction t = traxManager.getActiveTransaction(xid);

        if(t == null){
            //TODO: print error
            return false;
        }
        boolean[] relatedRM = t.getRelatedRMs();

     //   Trace.info("Resource=" + resources);
        if (relatedRM[0]){
          
            if(!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, "RM: Not a valid transaction");

            Transaction transaction = traxManager.getActiveTransaction(xid);
            RMHashMap m = transaction.get_TMPdata();

            synchronized (flightRM.m_data ){
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    flightRM.m_data.put(key, m.get(key));
                }
            }
            traxManager.removeActiveTransaction(xid);
        
        }
        if (relatedRM[1]){
            
            if(!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, "RM: Not a valid transaction");

            Transaction transaction = traxManager.getActiveTransaction(xid);
            RMHashMap m = transaction.get_TMPdata();

            synchronized (flightRM.m_data) {
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    flightRM.m_data.put(key, m.get(key));
                }
            }
            traxManager.removeActiveTransaction(xid);
        }
        if (relatedRM[2]){
             
            if(!traxManager.isActive(xid))
            throw new InvalidTransactionException(xid, "RM: Not a valid transaction");

            Transaction transaction = traxManager.getActiveTransaction(xid);
            RMHashMap m = transaction.get_TMPdata();

            synchronized (flightRM.m_data) {
                Set<String> keyset = m.keySet();
                for (String key : keyset) {
                    System.out.println("Write:(" + key + "," + m.get(key) + ")");
                    flightRM.m_data.put(key, m.get(key));
                }
            }
            traxManager.removeActiveTransaction(xid);
        }

        //if it is customer, we need all resources managers to work
        if (relatedRM[0] && relatedRM[1] && relatedRM[2]) {
            // RMHashMap m = t.getData();
            // synchronized (m_data) {
            //     Set<String> keyset = m.keySet();
            //     for (String key : keyset) {
            //         System.out.print("Write:(" + key + "," + m.get(key) + ")");
            //         m_data.put(key, m.get(key));
            //     }
            // }
        }
        endTransaction(xid, true);
        return true;
    }

    // public boolean abort(int xid) throws RemoteException, InvalidTransactionException {
    //     System.out.println("Abort transaction:" + xid);
    //     try {
    //         //checkTransaction(xid);
    //     } catch(TransactionAbortedException e) {
    //         throw new InvalidTransactionException(xid, "transaction has been aborted already");
    //     }

    //     Transaction t = traxManager.getActiveTransaction(xid);

    //     if(t == null){
    //         //TODO: print error
    //         return false;
    //     }

    //      boolean[] relatedRM = t.getRelatedRMs();

    //  //   Trace.info("Resource=" + resources);
    //     if (relatedRM[0]){
    //         flightRM.abort(xid);
    //     }
    //     if (relatedRM[1]){
    //         carRM.abort(xid);
    //     }
    //     if (relatedRM[2]){
    //          roomRM.abort(xid);
    //     }
    //     endTransaction(xid, false);
    //     return true;
    // }

    private void endTransaction(int xid, boolean commit) throws RemoteException {
        // Move to inactive transactions
        // TODO: remove commit parameter
        traxManager.removeActiveTransaction(xid);
        // traxManager.writeActiveData(xid, null);
        // traxManager.writeInactiveData(xid, new Boolean(commit));

        lockManager.UnlockAll(xid);
    }

    // private void updateTimeToLive(int xid) {
    //     traxManager.readActiveData(xid).updateLastAction();
    // }

    public boolean shutdown() throws RemoteException {
       
        carRM.shutdown();
        roomRM.shutdown();
        flightRM.shutdown();

        new Thread() {
            @Override
            public void run() {
                System.out.print("Shutting down...");
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                }
                System.out.println("done");
                System.exit(0);
            }

        }.start();
        return true;
    }

    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
        int id = xid;
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();

        Trace.info("addFlight - Redirect to Flight Resource Manager");
        //checkTransaction(id);
        acquireLock(id, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Flight");

        return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int xid, String location, int numCars, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("addCars - Redirect to Car Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Car");


        return carRM.addCars(id, location, numCars, price);

    }

    public boolean addRooms(int xid, String location, int numRooms, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("addRooms - Redirect to Room Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Room");

        return roomRM.addRooms(id, location, numRooms, price);

    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("deleteFlight - Redirect to Flight Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Flight");

        return flightRM.deleteFlight(id, flightNum);

    }

    public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("deleteCars - Redirect to Car Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Car");

        return carRM.deleteCars(id, location);
    }

    public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("deleteRooms - Redirect to Room Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Room");

        return roomRM.deleteRooms(id, location);
    }


    public int queryFlight(int xid, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryFlight - Redirect to Flight Resource Manager");
        //checkTransaction(id);
        Transaction trx = traxManager.getActiveTransaction(xid);
        trx.resetTimer();

        acquireLock(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Flight");
        return flightRM.queryFlight(id, flightNumber);
    }


    public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryCars - Redirect to Car Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Car");

        return carRM.queryCars(id, location);
    }



    public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        //checkTransaction(xid);
        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,"Customer");
        return super.queryCustomerInfo(xid,customerID);
    }


    public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryRooms - Redirect to Room Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Room");

        return roomRM.queryRooms(id, location);
    }


    public int queryFlightPrice(int xid, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryFlightPrice - Redirect to Flight Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Flight");

        return flightRM.queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryCarsPrice - Redirect to Car Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Car");

        return carRM.queryCarsPrice(id, location);
    }

    public int newCustomer(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        //checkTransaction(xid);

        Trace.info("RM::newCustomer(" + xid + ") called");
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        acquireLock(xid, customer.getKey(), TransactionLockObject.LockType.LOCK_WRITE);
        addResourceManagerUsed(id,"Customer");
        writeData(xid, customer.getKey(), customer);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");

        int id = xid;
        //checkTransaction(xid);
        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");

        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            customer = new Customer(customerID);
            writeData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
            return true;
        }
        else
        {
            Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }



    public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        //checkTransaction(xid);

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        }
        else {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                String type = reservedKey.split("-")[0];
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                if (type.equals("flight")) {
                    acquireLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    addResourceManagerUsed(id,"Flight");
                    flightRM.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                } else if (type.equals("car")) {
                    acquireLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    addResourceManagerUsed(id,"Car");
                    carRM.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                } else if (type.equals("room")) {
                    acquireLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    addResourceManagerUsed(id,"Room");
                    roomRM.removeReservation(xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                } else
                    Trace.error("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--reservedKey (" + reservedKey + ") wasn't of expected type.");

            }
            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }

    }
//--




    public boolean reserveFlight(int xid, int customerID, int flightNumber) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        String key = Flight.getKey(flightNumber);

        Trace.info("RM::reserveFlight(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        //checkTransaction(xid);
        // Check customer exists

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--customer doesn't exist");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(xid,"Flight");
        int price = flightRM.itemsAvailable(xid, key, 1);

        if (price < 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--item unavailable");
            return false;
        }
        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        if (flightRM.reserveFlight(xid, customerID, flightNumber)) {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            customer.reserve(key, String.valueOf(flightNumber), price);
            writeData(xid, customer.getKey(), customer);
            return true;
        }
        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--Could not reserve item");
        return false;

    }


    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        String key = Car.getKey(location);

        Trace.info("RM::reserveCar(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        //checkTransaction(xid);
        // Check customer exists

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Car");
        int price = carRM.itemsAvailable(xid, key, 1);

        if (price < 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        if (carRM.reserveCar(xid, customerID, location)) {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            customer.reserve(key, location, price);
            writeData(xid, customer.getKey(), customer);
            return true;
        }
        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
        return false;

    }


    public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("queryRoomsPrice - Redirect to Room Resource Manager");
        //checkTransaction(id);

        acquireLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Room");

        return roomRM.queryRoomsPrice(id, location);
    }

   

    
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        String key = Room.getKey(location);

        Trace.info("RM::reserveRoom(" + xid + ", customer=" + customerID + ", " + key + ") called" );
        //checkTransaction(xid);
        // Check customer exists

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Room");
        int price = roomRM.itemsAvailable(xid, key, 1);

        if (price < 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
            return false;
        }

        acquireLock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        if (roomRM.reserveRoom(xid, customerID, location)) {
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            customer.reserve(key, location, price);
            writeData(xid, customer.getKey(), customer);
            return true;
        }
        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
        return false;
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException, InvalidTransactionException
    {
        int id = xid;
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called" );
        //checkTransaction(xid);

        acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);
        addResourceManagerUsed(id,"Customer");
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
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Car");
            carPrice = carRM.itemsAvailable(xid, Car.getKey(location), 1);

            if (carPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                return false;
            }

            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Room");
            roomPrice = roomRM.itemsAvailable(xid, Room.getKey(location), 1);

            if (roomPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                return false;
            }

            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            roomRM.reserveRoom(xid, customerID, location);

            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
            customer.reserve(Room.getKey(location), location, roomPrice);

            writeData(xid, customer.getKey(), customer);

            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
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
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Car");
            carPrice = carRM.itemsAvailable(xid, Car.getKey(location), 1);

            if (carPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " doesn't have enough spots");
                return false;
            }
            acquireLock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            carRM.reserveCar(xid, customerID, location);

            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
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
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
                int price = flightRM.itemsAvailable(xid, Flight.getKey(keyInt), countraxManagerap.get(key));

                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + key + " doesn't have enough spots");
                    return false;
                } else {
                    flightPrice.put(keyInt, price);
                }
            }
            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Room");
            roomPrice = roomRM.itemsAvailable(xid, Room.getKey(location), 1);

            if (roomPrice < 0) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " doesn't have enough spots");
                return false;
            }
            acquireLock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            roomRM.reserveRoom(xid, customerID, location);

            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
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
                acquireLock(xid, Flight.getKey(keyInt), TransactionLockObject.LockType.LOCK_READ);
                addResourceManagerUsed(id,"Flight");
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
            acquireLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            addResourceManagerUsed(id,"Customer");
        }
        // Reserve flights
        for (Integer key : flightPrice.keySet()) {
            for (int i = 0; i < countraxManagerap.get(String.valueOf(key)); i++) {
                int price = flightPrice.get(key);

                acquireLock(xid, Flight.getKey(key), TransactionLockObject.LockType.LOCK_WRITE);
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
        int id = xid;

      //  //checkTransaction(xid);
        Transaction t = traxManager.getActiveTransaction(xid);
        RMHashMap m = t.get_TMPdata();
        Set<String> keyset = new HashSet<String>(m.keySet());
        keyset.addAll(m_data.keySet());

        String summary = "";

        for (String key: keyset) {
            String type = key.split("-")[0];
            if (!type.equals("customer"))
                continue;
            acquireLock(xid, key, TransactionLockObject.LockType.LOCK_READ);
            addResourceManagerUsed(id,"Customer");
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
                        case "Flight": {
                            flightRM = (IResourceManager)registry.lookup(name);
                            break;
                        }
                        case "Car": {
                            carRM = (IResourceManager)registry.lookup(name);
                            break;
                        }
                        case "Room": {
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

    public void acquireLock(int xid, String data, TransactionLockObject.LockType lockType) throws RemoteException, TransactionAbortedException, InvalidTransactionException{
        try {
            boolean lock = lockManager.Lock(xid, data, lockType);
            if (!lock) {
                Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") Unable to lock");
                throw new InvalidTransactionException(xid, "LockManager-Unable to lock");
            }
        } catch (DeadlockException e) {
            Trace.info("LM::lock(" + xid + ", " + data + ", " + lockType + ") " + e.getLocalizedMessage());
//            Transaction t = traxManager.getActiveTransaction(xid);
//            t.resetTimer();
            traxManager.abort(xid);
            throw new TransactionAbortedException(xid, "The transaction has been aborted due to a deadlock");
        }
    }

    public void addResourceManagerUsed(int xid, String resource) throws RemoteException  {
        Transaction t = traxManager.getActiveTransaction(xid);
        t.setRelatedRM(resource);

        try {
            try {

                switch (resource) {
                    case "Flight": {
                        flightRM.addTransaction(xid);
                        break;
                    }
                    case "Car": {
                        carRM.addTransaction(xid);
                        break;
                    }
                    case "Room": {
                        roomRM.addTransaction(xid);
                        break;
                    }
                    case "Customer": {
                        this.addTransaction(xid);
                    }
                }

            } catch (ConnectException e) {

                switch (resource) {
                    case "Flight": {
                        connectServer("Flight", s_flightServer.host, s_flightServer.port, s_flightServer.name);
                        flightRM.addTransaction(xid);
                        break;
                    }
                    case "Car": {
                        connectServer("Car", s_carServer.host, s_carServer.port, s_carServer.name);
                        carRM.addTransaction(xid);
                        break;
                    }
                    case "Room": {
                        connectServer("Room", s_roomServer.host, s_roomServer.port, s_roomServer.name);
                        roomRM.addTransaction(xid);
                        break;
                    }
                    case "Customer": {
                        this.addTransaction(xid);
                    }
                }
            }
        } catch (Exception e) {
            Trace.error(e.toString());
        }

    }





}
