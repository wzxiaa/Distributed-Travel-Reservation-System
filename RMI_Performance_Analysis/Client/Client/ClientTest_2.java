package Client;

import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.util.*;
import java.io.*;

public class ClientTest_2 extends Client implements Runnable
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 12345;
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group_24_";

    private static int num_clients = 6;
    private static int desired_thput = 50;
    private long timer = 0;
    private int x = 10;
    private static List<long[]> testRes = new ArrayList<>();

    public static void main(String args[])
    {
        if (args.length > 0)
        {
            s_serverHost = args[0];
        }
        if (args.length > 1)
        {
            s_serverName = args[1];
        }
        if (args.length > 2)
        {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            System.out.println("Starting test with desired throughput:" + desired_thput + " and " + num_clients + " clients...");
            ClientTest_2[] clientTests = new ClientTest_2[num_clients];
            Thread[] clientThreads = new Thread[num_clients];
            for(int i=0; i<num_clients; i++){
                clientTests[i]= new ClientTest_2();
                clientTests[i].connectServer();
                clientTests[i].setTimer();
		        if(i==0) clientTests[i].addMultipleFlights(300);
                clientThreads[i] = new Thread(clientTests[i]);
                clientThreads[i].start();
            }

            for(int i=0; i<num_clients; i++){
                clientThreads[i].join();
            }

            long total_res = 0;

            for(int i=0; i<testRes.size(); i++) {
                total_res += testRes.get(i)[0];
            }

            System.out.println("average response time : " + total_res/testRes.size());
        }

        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run(){
        int timeInterval = num_clients * 1000 / desired_thput;
        while(this.timer + 3000 < (new Date()).getTime()) {
        }
        boolean plus = true;
        for(int i=0; i<50; i++){
            if(plus) timeInterval += x;
            else timeInterval -= x;
            plus = !plus;
            try{
                int index = (int)(Math.random()*100 + 1);
                long[] res = runE1SingleRM(index);
                int sleepTime = (int)(timeInterval-res[0]);
                System.out.println("sleep time : " + sleepTime);
                System.out.println();
                testRes.add(res);
                if(sleepTime < 0) continue;
                else {
                    Thread.sleep(timeInterval-res[0]);
                }
            } catch(Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    public void addMultipleFlights(int num) throws Exception {
        try{
           System.out.println("Test 2: adding flights and customers ...");
           long[] st = m_resourceManager.start();
           int xid = (int) st[0];
           for(int i=1; i<=num; i++){
              m_resourceManager.addFlight(xid, i, 10000, 10+i);
           }
           for(int i=1; i<=num; i++){
              m_resourceManager.newCustomer(xid, i);
           }
            m_resourceManager.commit(xid);
        } catch(Exception e){
            System.out.println(e.toString());
            System.exit(-1);
        }
        System.out.println("Test 2: finished setting up");
    }

    public long[] runE1SingleRM(int index) throws Exception {
        ArrayList<long[]> total = new ArrayList<>();
        long startTime =  System.currentTimeMillis();
        int xid = -1;
        synchronized(m_resourceManager){
            startTime =  System.currentTimeMillis();
            long[] start = m_resourceManager.start();
            startTime = System.currentTimeMillis() - startTime;
            xid = (int) start[0];
        }
        if(xid == -1.0) {
            System.out.println("Starting failed, return");
            return new long[]{-1, -1, -1};
        } else {
            long addcartime = System.currentTimeMillis();
            synchronized(m_resourceManager){
            addcartime = System.currentTimeMillis();
                    m_resourceManager.queryFlight(xid, index);
                    addcartime = System.currentTimeMillis() - addcartime;
            }
            long querycartime = System.currentTimeMillis();;
            synchronized(m_resourceManager){
                querycartime = System.currentTimeMillis();
                m_resourceManager.reserveFlight(xid, index, index);
                querycartime = System.currentTimeMillis() - querycartime;
            }

            long commitTime =  System.currentTimeMillis();
            synchronized(m_resourceManager){
                commitTime =  System.currentTimeMillis();
                m_resourceManager.commit(xid);
                commitTime =  System.currentTimeMillis() - commitTime;
            }
            long totalResponseTime = addcartime+querycartime + commitTime + startTime;
            System.out.println("totalResponseTime : " + totalResponseTime);
            return new long[]{totalResponseTime};
        }
    }

    public void setTimer(){
        this.timer = (new Date()).getTime();
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
