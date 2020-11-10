package Server.TransactionManager;

import Server.Interface.InvalidTransactionException;
import Server.Middleware.Middleware;
import java.util.*;
import Server.Common.*;

public class TransactionManager implements Runnable{
    private HashMap<Integer, Transaction> activeTxns;
    private int counter;

    public TransactionManager(){    // Middleware mdw
        this.activeTxns = new HashMap<Integer, Transaction>();
        this.counter = 0;
    }

    public int start(){
        this.counter++;
        int xid = this.counter;
        synchronized (activeTxns) {
            this.activeTxns.put(xid, new Transaction(xid));
        }
        Thread th = new Thread(this);
        th.start();
        return xid;
    }

    @Override
    public void run() {
        while(true){
            try {
                synchronized (activeTxns) {
                    for (int xid : activeTxns.keySet()) {
                        if (activeTxns.get(xid).hasExpired()) {
                            try {
                                abort(xid);
                            } catch(Exception e){
                                e.printStackTrace(System.out);
                            }
                        }
                    }
                }
//                Thread.sleep(1000);
            } catch (Exception e){
                e.printStackTrace(System.out);
            }
        }
    }

    public boolean isActive(int xid) {
        synchronized (activeTxns) {
            return activeTxns.containsKey(xid);
        }
    }

    public Transaction getActiveTransaction(int xid) {
        synchronized (activeTxns){
            if(isActive(xid))
                return activeTxns.get(xid);
        }
        return null;
    }

    public void addActiveTransaction(int xid, Transaction t){
        synchronized (activeTxns){
            activeTxns.put(xid, t);
        }
    }

    public void removeActiveTransaction(int xid){
        synchronized (activeTxns){
            activeTxns.remove(xid);
        }
    }

    public void abort(int xid) throws InvalidTransactionException{
        // TODO
        Trace.info("Transaction manager: Aborted transaction " + xid);
		if(!isActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");

        removeActiveTransaction(xid);
    }
}
