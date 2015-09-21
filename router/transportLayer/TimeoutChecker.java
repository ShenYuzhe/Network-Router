package transportLayer;

import java.util.Hashtable;
import java.util.Iterator;

public class TimeoutChecker implements Runnable {
	private static long timeoutPeriod = 1000;
	
	private TransportCallback managerCallback;
	private Hashtable<String, TransportHeader> eventQueue;
	public boolean exitFlag;

	public TimeoutChecker(TransportCallback newCallback) {
		// TODO Auto-generated constructor stub
		this.managerCallback = newCallback;
		this.eventQueue = new Hashtable<String, TransportHeader>();
		this.exitFlag = false;
	}
	
	private void EventScanner() {
		long currTime = System.currentTimeMillis();
		for (Iterator iter = this.eventQueue.keySet().iterator(); iter.hasNext();) {
			String destFile = (String)iter.next();
			TransportHeader event = eventQueue.get(destFile);
			if (currTime - event.sentTime >= this.timeoutPeriod)
				this.managerCallback.onTimeOut(event);
		}
	}
	
	public void insertEvent(TransportHeader header) {
		String key = header.destination + TransLayer.fieldSpliter + header.fileName;
		this.eventQueue.put(key, header);
	}
	
	public void removeEvent(TransportHeader header) {
		String key = header.destination + TransLayer.fieldSpliter + header.fileName;
		this.eventQueue.remove(key);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (!this.exitFlag) {
			try {
				Thread.sleep(TimeoutChecker.timeoutPeriod);
			} catch (Exception e) {}
			this.EventScanner();
		}
	}

}
