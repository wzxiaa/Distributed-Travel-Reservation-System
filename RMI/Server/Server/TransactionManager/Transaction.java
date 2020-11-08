package Server.TransactionManager;

import java.util.*;
import Server.Common.*;

public class Transaction {
    public static final String FLIGHT_RM = "Flight";
    public static final String ROOM_RM = "Room";
    public static final String CAR_RM = "Car";
    public static final long TIME_TO_LIVE = 20000;

    private int xid;
    protected RMHashMap tmpData = new RMHashMap();
    private long startTime;
    private boolean[] relatedRM;


    public Transaction(int xid){
        this.xid = xid;
        this.startTime = (new Date()).getTime();
        this.relatedRM = new boolean[3];
    }


    public RMHashMap get_TMPdata(){
		return tmpData;
    }
    
    public boolean hasExpired(){
        return (new Date()).getTime() > startTime + TIME_TO_LIVE ? true : false;
    }

    public void resetTimer() {
        this.startTime = (new Date()).getTime();
    }


    public void setRelatedRM(String rm) {
        switch (rm) {
            case FLIGHT_RM:
                relatedRM[0] = true;
                break;
            case ROOM_RM:
                relatedRM[1] = true;
                break;
            case CAR_RM:
                relatedRM[2] = true;
                break;
            default:
                break;
        }
    }

    public boolean[] getRelatedRMs(){
        return relatedRM;
    }

    // Reads a data item
    public RMItem readCopyData(int xid, String key)
    {
        synchronized(tmpData) {
            RMItem item = tmpData.get(key);
            if (item != null) {
                return (RMItem)item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    public void writeCopyData(int xid, String key, RMItem value)
    {
        synchronized(tmpData) {
            tmpData.put(key, value);
        }
    }

    // Remove the item out of storage
    protected void removeCopyData(int xid, String key)
    {
        synchronized(tmpData) {
            tmpData.remove(key);
        }
    }

    public int getXid(){
        return this.xid;
    }
}
