// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.Middleware.Middleware;
import Server.TransactionManager.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;
import Server.Common.RMHashMap;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	public RMHashMap m_data = new RMHashMap();
	public TransactionManager tm;

	public ResourceManager(String p_name) {
		m_name = p_name;
		tm = new TransactionManager();
	}

	protected void setTransactionManager(TransactionManager tm) {
		this.tm = tm;
	}

	public void putData(String key, RMItem value) throws RemoteException {
		synchronized(m_data){
			m_data.put(key, value);
		}
	}

	public RMHashMap getTraxData(int xid) throws RemoteException {
		return tm.getActiveTransaction(xid).get_TMPdata();
	}

	public void removeTrax(int xid) throws RemoteException {
		tm.removeActiveTransaction(xid);
	}

	public void addNewTrax(int xid) throws RemoteException {
		Trace.info("Server: adding new transaction " + xid);
		if (!tm.isActive(xid)) {
			Transaction t = new Transaction(xid);
			tm.addActiveTransaction(xid, t);
		}
	}

	// If the data exists from the previous committed transactions, we copy them into the current transaction.
	protected void writePreviousCommittedData(int xid, String key) {
		Transaction t = tm.getActiveTransaction(xid);
		synchronized (m_data) {
			if (m_data.get(key) != null) {
				t.writeCopyData(xid, key, (RMItem) m_data.get(key).clone());
			}
			else {
				t.writeCopyData(xid, key, null);
			}
		}
	}

	protected void removePreviousCommittedData(String key) {
		synchronized (m_data) {
			if (m_data.get(key) != null) {
				m_data.remove(key);
			}
		}
	}

	protected RMItem readData(int xid, String key) throws InvalidTransactionException
	{
		if(!tm.isActive(xid))
			throw new InvalidTransactionException(xid, " Server: Not a valid transaction");
		Transaction t = tm.getActiveTransaction(xid);
		if (t.readCopyData(xid, key)==null) {
			writePreviousCommittedData(xid, key);
		}
		return t.readCopyData(xid, key);
	}

	protected void writeData(int xid, String key, RMItem value) throws InvalidTransactionException
	{
		if(!tm.isActive(xid))
			throw new InvalidTransactionException(xid, " Server: Not a valid transaction");
		writePreviousCommittedData(xid, key);
		Transaction t = tm.getActiveTransaction(xid);
		t.writeCopyData(xid, key, value);
	}

	protected void removeData(int xid, String key) throws InvalidTransactionException
	{
		if(!tm.isActive(xid))
			throw new InvalidTransactionException(xid, " Server: Not a valid transaction");
		writePreviousCommittedData(xid, key);
		Transaction t = tm.getActiveTransaction(xid);
		t.removeCopyData(xid, key);
		removePreviousCommittedData(key);
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws InvalidTransactionException
	{
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum(int xid, String key) throws InvalidTransactionException
	{
		Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;
		if (curObj != null)
		{
			value = curObj.getCount();
		}
		Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
		return value;
	}

	// Query the price of an item
	protected int queryPrice(int xid, String key) throws InvalidTransactionException
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0;
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) throws InvalidTransactionException
	{
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		}

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{
			customer.reserve(key, location, item.getPrice());
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}
	}

	public int countAvailaleItems(int xid, String key, int quantity) throws InvalidTransactionException {
		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return -1;
		}
		else if (item.getCount() < quantity)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--Not enough items");
			return -1;
		}

		return item.getCount();
	}

	public int getPrice(int xid, String key) throws InvalidTransactionException {
		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return -1;
		} else {
			return item.getPrice();
		}
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
		Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
		if (curObj == null)
		{
			// Doesn't exist yet, add it
			Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
		}
		else
		{
			// Add seats to existing flight and update the price if greater than zero
			curObj.setCount(curObj.getCount() + flightSeats);
			if (flightPrice > 0)
			{
				curObj.setPrice(flightPrice);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
		}
		return true;
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException
	{
		Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Car curObj = (Car)readData(xid, Car.getKey(location));
		if (curObj == null)
		{
			// Car location doesn't exist yet, add it
			Car newObj = new Car(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
		}
		else
		{
			// Add count to existing car location and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
		Room curObj = (Room)readData(xid, Room.getKey(location));
		if (curObj == null)
		{
			// Room location doesn't exist yet, add it
			Room newObj = new Room(location, count, price);
			writeData(xid, newObj.getKey(), newObj);
			Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
		} else {
			// Add count to existing object and update price if greater than zero
			curObj.setCount(curObj.getCount() + count);
			if (price > 0)
			{
				curObj.setPrice(price);
			}
			writeData(xid, curObj.getKey(), curObj);
			Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
		}
		return true;
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Flight.getKey(flightNum));
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Car.getKey(location));
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return deleteItem(xid, Room.getKey(location));
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Flight.getKey(flightNum));
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Car.getKey(location));
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryNum(xid, Room.getKey(location));
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Flight.getKey(flightNum));
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Car.getKey(location));
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return queryPrice(xid, Room.getKey(location));
	}

	public String queryCustomerInfo(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			return "";
		}
		else
		{
			Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
			System.out.println(customer.getBill());
			return customer.getBill();
		}
	}

	public String Summary(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException{
		String summary = "";

		for (String key: m_data.keySet()) {
			String type = key.split("-")[0];
			System.out.println("type in rm"+type);
			if (!type.equals("customer"))
				continue;
			Customer customer = (Customer)readData(xid, key);
			summary += customer.getSummary();

		}
		return summary;
	}

	public int newCustomer(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ") called");
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
			String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
			String.valueOf(Math.round(Math.random() * 100 + 1)));
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
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
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer != null)
		{
			removeData(xid, customer.getKey());
			Trace.info("RM::customer(" + customerID + ") deleted");
			return true;
		}
		return false;
	}

	public boolean removeReservation(int xid, int customerID, String reserveditemKey, int reserveditemCount) throws RemoteException,TransactionAbortedException, InvalidTransactionException {
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditemKey + " " +  reserveditemCount +  " times");
		ReservableItem item  = (ReservableItem)readData(xid, reserveditemKey);
		Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditemKey + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
		item.setReserved(item.getReserved() - reserveditemCount);
		item.setCount(item.getCount() + reserveditemCount);
		writeData(xid, item.getKey(), item);
		return true;
	}

	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Car.getKey(location), location);
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return reserveItem(xid, customerID, Room.getKey(location), location);
	}

	// Reserve bundle
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException, InvalidTransactionException
	{
		return false;
	}

	

	public int start() throws RemoteException{
		return 0;
	}

	public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException{
		return false;
	}

	public void abort(int xid) throws RemoteException, InvalidTransactionException {
	}

	public boolean shutdown() throws RemoteException {
		new Thread() {
			@Override
			public void run() {
				System.out.print("Shutting down...");
				System.out.println("done");
				System.exit(0);
			}

		}.start();
		return true;
	}

//	public int start() throws RemoteException {
//		return -1;
//	}

//	public boolean commit(int xid) throws RemoteException,TransactionAbortedException, InvalidTransactionException
//	{
//		System.out.println("Commit transaction:" + xid);
//		//flush transaction to m_data
//		if(!tm.isActive(xid))
//			throw new InvalidTransactionException(xid, "RM: Not a valid transaction");
//
//		Transaction t = tm.getActiveTransaction(xid);
//		RMHashMap m = t.getData();
//
//		synchronized (m_data) {
//			Set<String> keyset = m.keySet();
//			for (String key : keyset) {
//				System.out.println("Write:(" + key + "," + m.get(key) + ")");
//				m_data.put(key, m.get(key));
//			}
//		}
//
//		// Move to inactive transactions
//		tm.addActiveTransaction(xid, null);
//		tm.writeInactiveData(xid, new Boolean(true));
//
//		return true;
//	}
//
//	public void abort(int xid) throws RemoteException, InvalidTransactionException {
//		System.out.println("Abort transaction:" + xid);
//
//		if(!tm.isActive(xid))
//			throw new InvalidTransactionException(xid, " Server: Not a valid transaction");
//
//		tm.addActiveTransaction(xid, null);
//		tm.writeInactiveData(xid, new Boolean(false));
//
//	}

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 
