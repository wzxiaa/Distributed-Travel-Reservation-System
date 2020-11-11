package Client;

import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.util.*;
import java.io.*;

public class ClientTest_1 extends Client
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 12345;
    private static String s_serverName = "Middleware";
    private static int method =-1;
    private static int iterations = 1;

    //TODO: ADD YOUR GROUP NUMBER TO COMPILE
    private static String s_rmiPrefix = "group_24_";

    public static void main(String args[])
    {
        if (args.length > 0)
        {
            method = Integer.parseInt(args[0]);
            iterations = Integer.parseInt(args[1]);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            ClientTest_1 client = new ClientTest_1();
            client.connectServer();
//            client.start();

            ArrayList<long[]> res = new ArrayList<>();

            System.out.println("Method : " + method);

            for(int i=1; i<1+iterations; i++) {
                if (method == 1) {
                    res.add(client.runE1SingleRM());
                } else if(method == 2) {
                    res.add(client.runE1MultipleRM());
                } else {
                    client.runTest();
                }
//                res.add(tmp);
            }

            long total_res = 0;
            long total_mdw = 0;
            long total_rm = 0;
            long total_db = 0;

            for(int i=0; i<iterations; i++) {
                total_res += res.get(i)[0];
                total_mdw += res.get(i)[1];
                total_rm += res.get(i)[2];
                total_db += res.get(i)[3];
            }

            long time_scale = iterations * 1000000;

            System.out.println("average response time : " + (double)(total_res/time_scale));
            System.out.println("average mdw time : " + (double)total_mdw/time_scale);
            System.out.println("average rm time : " + (double)total_rm/time_scale);
            System.out.println("average db time : " + (double)total_db/time_scale);
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }


    public long[] runE1SingleRM() throws Exception {
        long startTime = System.nanoTime();
        // int xid = m_resourceManager.start();
        ArrayList<long[]> total = new ArrayList<>();

        long[] start = m_resourceManager.start();
        int xid = (int)start[0];
        if(start[0] == -1.0) {
            System.out.println("Error starting! Aborted");
            return new long[]{-1, -1, -1};
        } else {
            total.add(start);
            total.add(m_resourceManager.addCars(xid, "Montreal", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Montreal"));
            total.add(m_resourceManager.addCars(xid, "Toronto", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Toronto"));
            total.add(m_resourceManager.addCars(xid, "Vancouver", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Vancouver"));
            total.add(m_resourceManager.addCars(xid, "Ottawa", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Ottawa"));
            total.add(m_resourceManager.addCars(xid, "Waterloo", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Waterloo"));
            total.add(m_resourceManager.commit(xid));

            long endTime = System.nanoTime();
            long totalResponseTime = endTime - startTime;
            long MDWTime = 0;
            long totalRMDBTime = 0;
            long totalRMTime = 0;
            long totalDBTime = 0;


            for (int i = 0; i < total.size(); i++) {
//                System.out.println("i: " + i);
//                for(int j=0; j<total.get(i).length; j++) {
//                    System.out.println(total.get(i)[j]);
//                }

                if(i!=0) MDWTime += total.get(i)[0];
                if(i==0) MDWTime += total.get(i)[1];

                if(i!=0 && i!=(total.size()-1)) {
                    totalRMDBTime += total.get(i)[1];
                    totalDBTime += total.get(i)[2];
                }

                if(i==(total.size()-1)) {
                    totalRMDBTime += total.get(i)[1];
                    totalDBTime += total.get(i)[1];
                }
            }

            totalRMTime = totalRMDBTime - totalDBTime;

            System.out.println("totalResponseTime : " + totalResponseTime);
            System.out.println("totalMDWTime : " + MDWTime);
            System.out.println("totalRMDBTime : " + totalRMDBTime);
            System.out.println("totalRMTime : " + totalRMTime);
            System.out.println("totalDBTime : " + totalDBTime);
//            System.out.println("totalCommunicationTime : " + totalCommunicationTime);
            return new long[]{totalResponseTime, MDWTime, totalRMTime, totalDBTime};
        }
    }


    public long[] runE1MultipleRM() throws Exception {
        long startTime = System.nanoTime();
        // int xid = m_resourceManager.start();
        ArrayList<long[]> total = new ArrayList<>();

        long[] start = m_resourceManager.start();
        int xid = (int)start[0];
        if(start[0] == -1.0) {
            System.out.println("Error starting! Aborted");
            return new long[]{-1, -1, -1};
        } else {
            total.add(start);
            total.add(m_resourceManager.addCars(xid, "Montreal", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Montreal"));
            total.add(m_resourceManager.addRooms(xid, "Toronto", 1000, 1000));
            total.add(m_resourceManager.queryRooms(xid, "Toronto"));
            total.add(m_resourceManager.addFlight(xid, 1, 1000, 1000));
            total.add(m_resourceManager.queryFlight(xid, 1));
            total.add(m_resourceManager.addCars(xid, "Ottawa", 1000, 1000));
            total.add(m_resourceManager.queryCars(xid, "Ottawa"));
            total.add(m_resourceManager.addFlight(xid, 2, 1000, 1000));
            total.add(m_resourceManager.queryFlight(xid, 2));
            total.add(m_resourceManager.commit(xid));

            long endTime = System.nanoTime();
            long totalResponseTime = endTime - startTime;
            long MDWTime = 0;
            long totalRMDBTime = 0;
            long totalRMTime = 0;
            long totalDBTime = 0;

            for (int i = 0; i < total.size(); i++) {
//                System.out.println(i);
//                System.out.println(total.get(i)[0]);
                if(i!=0) MDWTime += total.get(i)[0];
                if(i==0) MDWTime += total.get(i)[1];

                if(i!=0 && i!=(total.size()-1)) {
                    totalRMDBTime += total.get(i)[1];
                    totalDBTime += total.get(i)[2];
                }

                if(i==(total.size()-1)) {
                    totalRMDBTime += total.get(i)[1];
                    totalDBTime += total.get(i)[1];
                }
            }

            totalRMTime = totalRMDBTime - totalDBTime;

            System.out.println("totalResponseTime : " + totalResponseTime);
            System.out.println("totalMDWTime : " + MDWTime);
            System.out.println("totalRMDBTime : " + totalRMDBTime);
            System.out.println("totalRMTime : " + totalRMTime);
            System.out.println("totalDBTime : " + totalDBTime);
//            System.out.println("totalCommunicationTime : " + totalCommunicationTime);
            return new long[]{totalResponseTime, MDWTime, totalRMTime, totalDBTime};
        }
    }


    public long[] runTest() throws Exception {
        long startTime = System.currentTimeMillis();
        // int xid = m_resourceManager.start();
        ArrayList<long[]> total = new ArrayList<>();

        long[] start = m_resourceManager.start();
        int xid = (int)start[0];

        int cid = 10000;

        m_resourceManager.newCustomer(xid, cid);


        if(start[0] == -1.0) {
            System.out.println("Error starting! Aborted");
            return new long[]{-1, -1, -1};
        } else {
            total.add(start);
            total.add(m_resourceManager.addCars(xid, "Montreal", 1000, 1000));
            total.add(m_resourceManager.commit(xid));
//            total.add(m_resourceManager.queryCars(xid, "Montreal"));
//            total.add(m_resourceManager.addRooms(xid, "Toronto", 1000, 1000));
//            total.add(m_resourceManager.queryRooms(xid, "Toronto"));
//            total.add(m_resourceManager.addFlight(xid, 1, 1000, 1000));
//            total.add(m_resourceManager.queryFlight(xid, 1));
//            total.add(m_resourceManager.reserveCar(xid, cid, "Montreal"));
//            total.add(m_resourceManager.reserveRoom(xid, cid, "Toronto"));
//            total.add(m_resourceManager.reserveFlight(xid, cid, 1));
//            total.add(m_resourceManager.commit(xid));

//            total.add(m_resourceManager.addCars(xid, "Ottawa", 1000, 1000));
//            total.add(m_resourceManager.queryCars(xid, "Ottawa"));
//            total.add(m_resourceManager.addFlight(xid, 2, 1000, 1000));
//            total.add(m_resourceManager.queryFlight(xid, 2));

            long endTime = System.currentTimeMillis();
            long totalResponseTime = endTime - startTime;
            long MDWTime = 0;
            long totalRMDBTime = 0;
            long totalRMTime = 0;
            long totalDBTime = 0;

//            for (int i = 0; i < 10; i++) {
////                System.out.println(i);
//                System.out.println(total.get(i)[1]);
//                if(i!=0) MDWTime += total.get(i)[0];
//                totalRMDBTime += total.get(i)[1];
//                if(i!=0) totalDBTime += total.get(i)[2];
//            }

            for(int i=0; i< total.get(2).length; i++) {
                System.out.println(total.get(2)[i]);
            }


//            totalRMTime = totalRMDBTime - totalDBTime;
//
//            System.out.println("totalResponseTime : " + totalResponseTime);
//            System.out.println("totalMDWTime : " + MDWTime);
//            System.out.println("totalRMDBTime : " + totalRMDBTime);
//            System.out.println("totalRMTime : " + totalRMTime);
//            System.out.println("totalDBTime : " + totalDBTime);
//            System.out.println("totalCommunicationTime : " + totalCommunicationTime);
            return new long[]{totalResponseTime, MDWTime, totalRMTime, totalDBTime};
        }
    }


//    public void runE1MultipleRM() throws Exception {
//        long startTime = System.currentTimeMillis();
//        int xid = m_resourceManager.start();
//        ArrayList<ArrayList<long>> total = new ArrayList<>();
//        total.add(m_resourceManager.addCars(xid, "Montreal", 1000, 1000));
//        total.add(m_resourceManager.queryCars(xid, "Montreal"));
//        total.add(m_resourceManager.addRooms(xid, "Toronto", 1000, 1000));
//        total.add(m_resourceManager.queryRooms(xid, "Toronto"));
//        total.add(m_resourceManager.addFlight(xid, 1, 1000, 1000));
//        total.add(m_resourceManager.queryFlight(xid, 1));
//        total.add(m_resourceManager.addCars(xid, "Ottawa", 1000, 1000));
//        total.add(m_resourceManager.queryCars(xid, "Ottawa"));
//        total.add(m_resourceManager.addFlight(xid, 2, 1000, 1000));
//        total.add(m_resourceManager.queryFlight(xid, 2));
//        long endTime = System.currentTimeMillis();
//        long totalResponseTime = endTime - startTime;
//        long totalMDWTime, totalDBTime, totalCommunicationTime = 0;
//        for (int i=0; i<10; i++) {
//            totalMDWTime += total.get(i).get(0);
//            totalDBTime += total.get(i).get(1);
//            totalCommunicationTime += total.get(i).get(2);
//        }
//        System.out.println("totalResponseTime : " + totalResponseTime);
//        System.out.println("totalMDWTime : " + totalMDWTime);
//        System.out.println("totalDBTime : " + totalDBTime);
//        System.out.println("totalCommunicationTime : " + totalCommunicationTime);
//    }



    public ClientTest_1()
    {
        super();
    }

    public void connectServer()
    {
        connectServer(s_serverHost, s_serverPort, s_serverName);
    }

    public void connectServer(String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    m_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
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
}
