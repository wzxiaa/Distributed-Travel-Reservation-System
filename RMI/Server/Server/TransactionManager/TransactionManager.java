package Server.TransactionManager;
import Server.Interface.InvalidTransactionException;
import Server.Middleware.Middleware;

import java.util.*;
public class TransactionManager implements Runnable{
    private HashMap<Integer, Transaction> activeTxns;
//    private HashMap<Integer, Transaction> abortedTxns;
//    private HashMap<Integer, Transaction> committedTxns;
//    public Middleware mdw;
    private int counter;

    public TransactionManager(){    // Middleware mdw
        this.activeTxns = new HashMap<Integer, Transaction>();
//        this.abortedTxns = new HashMap<Integer, Transaction>();
//        this.committedTxns = new HashMap<Integer, Transaction>();
//        this.mdw = mdw;
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
                                System.out.println("Aborted!!!!!!!!!!!");
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
        System.out.println("Abort transaction:" + xid);
        System.out.println("Aborted!!!!!!!!!!!");
		if(!isActive(xid))
			throw new InvalidTransactionException(xid, "Not a valid transaction");

        removeActiveTransaction(xid);
		//removeActiveTrcccansaction(xid, null);
//		writeInactiveData(xid, new Boolean(false));
    }

//    public void addAbortedTransaction(int xid, Transaction t){
//        synchronized (abortedTxns){
//            abortedTxns.put(xid, t);
//        }
//    }
//
//    public void addCommittedTransaction(int xid, Transaction t){
//        synchronized (committedTxns){
//            committedTxns.put(xid, t);
//        }
//    }
//
//    public boolean isAborted(int xid) {
//        synchronized (abortedTxns) {
//            return abortedTxns.containsKey(xid);
//        }
//    }
//
//    public boolean isCommitted(int xid) {
//        synchronized (committedTxns) {
//            return committedTxns.containsKey(xid);
//        }
//    }
}
