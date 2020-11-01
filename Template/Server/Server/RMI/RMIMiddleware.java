// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;


import Server.Common.*;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleware extends ResourceManager
{
	
    private static String m_serverName ="Middleware";
    private static String m_rmiPrefix = "group_24_";

    private static int middleware_port = 3024;
    private static int server_port_car = 3124; 
    private static int server_port_room = 3224;
    private static int server_port_flight = 3324;
	private static String serverHost_Car;
	private static String serverHost_Room;
	private static String serverHost_Flight;

    private static IResourceManager flightRM = null;
    private static IResourceManager carRM = null;
    private static IResourceManager roomRM  = null;

    private Queue<Integer> customerIdx;

	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();

    private static String s_serverName = "Server";
	//TODO: ADD YOUR GROUP NUMBER TO COMPLETE
	private static String s_rmiPrefix = "group_24_";

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			serverHost_Car=args[1];
			serverHost_Room=args[2];
			serverHost_Flight=args[0];
			s_serverName = args[3];
		}
			
		// Create the RMI server entry
		try {
			// Create a new Server object
			RMIMiddleware server = new RMIMiddleware(s_serverName);

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(3024);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(3024);
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
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}


	public RMIMiddleware(String p_name)
	{
		super(p_name);
		m_name = p_name;
	
				try {
					Registry carRegistry = LocateRegistry.getRegistry(serverHost_Car, server_port_car);
                    carRM = (IResourceManager) carRegistry.lookup(m_rmiPrefix + "Cars");
                    if (carRM == null)
                        throw new AssertionError();

					Registry roomRegistry = LocateRegistry.getRegistry(serverHost_Room, server_port_room);
                    roomRM = (IResourceManager) roomRegistry.lookup(m_rmiPrefix + "Rooms");
                    if (roomRM == null)
                        throw new AssertionError();
	
					Registry flightRegistry = LocateRegistry.getRegistry(serverHost_Flight, server_port_flight);
                    flightRM = (IResourceManager) flightRegistry.lookup(m_rmiPrefix + "Flights");
                    if (flightRM == null)
                        throw new AssertionError();
				}
				catch (NotBoundException|RemoteException e) {
					System.out.println(e);
				}
	}


	@Override
	public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
		throws RemoteException {
		return flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
	}

	
	@Override
	public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {

			System.out.println("carRM"+carRM);
	
		return carRM.addCars(id, location, numCars, price);
	}

	@Override
 	public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
    return roomRM.addRooms(id, location, numRooms, price);
  	}

	
	@Override
	public boolean deleteFlight(int id, int flightNum) throws RemoteException {
		return flightRM.deleteFlight(id, flightNum);
	}

	@Override
	public boolean deleteCars(int id, String location) throws RemoteException {
		return carRM.deleteCars(id, location);
	}

	@Override
	public boolean deleteRooms(int id, String location) throws RemoteException {
		return roomRM.deleteRooms(id, location);
	}

	@Override
  public int queryFlight(int id, int flightNumber) throws RemoteException {
    return flightRM.queryFlight(id, flightNumber);
  }

  @Override
  public int queryCars(int id, String location) throws RemoteException {
    return carRM.queryCars(id, location);
  }

  @Override
  public int queryRooms(int id, String location) throws RemoteException {
    return roomRM.queryRooms(id, location);
  }

  @Override
  public String queryCustomerInfo(int id, int customerID) throws RemoteException {
    return flightRM.queryCustomerInfo(id, customerID)
        + carRM.queryCustomerInfo(id, customerID).split("\n", 2)[1] + roomRM.queryCustomerInfo(id, customerID).split("\n", 2)[1];
  }

  @Override
  public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
    return flightRM.queryFlightPrice(id, flightNumber);
  }

  @Override
  public int queryCarsPrice(int id, String location) throws RemoteException {
    return carRM.queryCarsPrice(id, location);
  }

  @Override
  public int queryRoomsPrice(int id, String location) throws RemoteException {
    return roomRM.queryRoomsPrice(id, location);
  }

  @Override
  public int newCustomer(int id) throws RemoteException {
    int cid = super.newCustomer(id);
    flightRM.newCustomer(id, cid);
    carRM.newCustomer(id, cid);
    roomRM.newCustomer(id, cid);
    return cid;
  }

  @Override
  public boolean newCustomer(int id, int cid) throws RemoteException {
    return super.newCustomer(id, cid) && flightRM.newCustomer(id, cid) && carRM.newCustomer(id, cid) && roomRM.newCustomer(id, cid);
  }

  @Override
  public boolean deleteCustomer(int xid, int customerID) throws RemoteException{
	  return carRM.deleteCustomer(xid, customerID) && roomRM.deleteCustomer(xid,customerID) 
	  && flightRM.deleteCustomer(xid, customerID);
  }

  @Override
  public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
    return flightRM.reserveFlight(id, customerID, flightNumber);
  }

  @Override
  public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
    return carRM.reserveCar(id, customerID, location);
  }

  @Override
  public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
    return roomRM.reserveRoom(id, customerID, location);
  }

  @Override
  public String Summary(int id) throws RemoteException {
    // String result = "";
    // String carReserve = carRM.Summary(id);
    // System.out.println("car: " + carReserve);
    // String roomReserve = roomRM.Summary(id);
    // System.out.println("room: " + roomReserve);
    // String flightReserve = flightRM.Summary(id);
    // System.out.println("flight: " + flightReserve);
    // return result.concat(carReserve).concat(roomReserve).concat(flightReserve);
    String s = "";
    HashMap<Integer,String> cars = carRM.getReserved(id);
    HashMap<Integer,String> rooms = roomRM.getReserved(id);
    HashMap<Integer,String> flights = flightRM.getReserved(id);	
    for(Integer key: cars.keySet()) {
	s = s.concat("Customer: "+key+"\n");
    	System.out.println(cars.get(key));
    	System.out.println(rooms.get(key));
    	System.out.println(flights.get(key));
    	s = s.concat(cars.get(key)).concat(rooms.get(key)).concat(flights.get(key)).concat("\n");
    }
    return s;
  }
  @Override
  public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location,
      boolean car, boolean room) throws RemoteException {
    	boolean res = true;
		for (String fn:flightNumbers) res &=reserveFlight(id,customerID,Integer.parseInt(fn));
		if (car) res &= reserveCar(id, customerID, location);
		if (room) res &= reserveRoom(id, customerID, location);
		return res; // return False if any of the above failed
  }

	public String getName() throws RemoteException
	{
		return m_name;
	}
}
 




