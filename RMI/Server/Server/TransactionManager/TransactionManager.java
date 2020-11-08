package Server.TransactionManager;

import java.util.*;
public class TransactionManager implements Runnable{
    private HashMap<Integer, Transaction> activeTxns;
//    private HashMap<Integer, Transaction> abortedTxns;
//    private HashMap<Integer, Transaction> committedTxns;
    public Middleware mdw;
    private int counter;

    public TransactionManager(Middleware mdw){
        this.activeTxns = new HashMap<Integer, Transaction>();
//        this.abortedTxns = new HashMap<Integer, Transaction>();
//        this.committedTxns = new HashMap<Integer, Transaction>();
        this.mdw = mdw;
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
                            this.mdw.abort(xid);
                        }
                    }
                }
                Thread.sleep(5000);
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
