package networkLayer;

import java.util.Hashtable;
import java.util.Iterator;

public class TimeManager implements Runnable{
	private long updatePeriod = 10000;
	private long expirePeriod = 30000;
	private NetworkCallback managerCallback;
	private Hashtable<String, NetworkEvent> eventQueue;

	public boolean exitFlag;
	
	public void setUpdatePeriod(int newPeriod) {
		this.updatePeriod = newPeriod * 1000;
		this.expirePeriod = 3 * this.updatePeriod;
	}
	
	public TimeManager(NetworkCallback newCallback) {
		// TODO Auto-generated constructor stub
		this.eventQueue = new Hashtable<String, NetworkEvent>();
		this.managerCallback = newCallback;
		this.exitFlag = false;
	}

	private void EventScanner() {
		long currTime = System.currentTimeMillis();
		for (Iterator iter = eventQueue.keySet().iterator(); iter.hasNext();) {
			String hostName = (String)iter.next();
			NetworkEvent event = eventQueue.get(hostName);
			if (event.operation.equals("UPDATE") && currTime - event.createTime >= updatePeriod) {
				this.managerCallback.onVectorUpdate();
			} else if (event.operation.equals("EXPIRE") && currTime - event.createTime >= expirePeriod)
				this.managerCallback.onNeighborExpire(event.hostName);
		}
	}
	
	public void insertEvent(NetworkEvent event) {
		eventQueue.put(event.hostName, event);
	}
	
	public void removeEvent(String hostName) {
		eventQueue.remove(hostName);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (!exitFlag) {
			try {
				Thread.sleep(1000);
				EventScanner();
			} catch (Exception e) {}
			
		}
	}

}
