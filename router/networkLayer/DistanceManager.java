package networkLayer;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class DistanceManager {
	private String localAddr = "";
	
	private Hashtable<String, node> localVector;
	private Hashtable<String, Double> weightVector;
	private Hashtable<String, Double> weightCopy;
	private Hashtable<String, Vector<node> > distanceTable;
	private HashSet<String> killonce_list;
	private NetworkCallback managerCallback;
	private int ThresHold; // Any distance exceed this value should be discarded.
	
	public DistanceManager(NetworkCallback newCallback) {
		// TODO Auto-generated constructor stub
		localVector = new Hashtable<String, node>();
		weightVector = new Hashtable<String, Double>();
		weightCopy = new Hashtable<String, Double>();
		distanceTable = new Hashtable<String, Vector<node> >();
		killonce_list = new HashSet<String>();
		localVector.put(localAddr, new node(0.0, localAddr, localAddr));
		managerCallback = newCallback;
		
		ThresHold = 200;
	}
	
	public void setThresHold(int newThesHold) {
		this.ThresHold = newThesHold;
	}
	
	public boolean isNeighborValidt(String neighbor) {
		if (this.weightVector.containsKey(neighbor))
			return true;
		return false;
	}
	
	public boolean isDestValid(String destination) {
		return this.localVector.containsKey(destination);
	}
	
	public boolean isOriginalNeighbor(String neighbor) {
		return this.weightCopy.containsKey(neighbor);
	}
	
	public void setLocalAddr(String newAddr) {
		this.localAddr = newAddr;
	}
	
	public void setWeight(String address, Double weight) {
		weightVector.put(address, weight);
	}

	public void resetSelfVec() {
		localVector.clear();
		localVector.put(localAddr, new node(0.0, localAddr, localAddr));
	}

	public void printlocalVector() {
		System.out.println(System.currentTimeMillis() + " Distance vector list is:");
		for (Iterator iter = localVector.keySet().iterator(); iter.hasNext();) {
			String destination = (String)iter.next();
			node CurrDest = localVector.get(destination);
			System.out.println("Destination = " + destination 
					+ " Cost = " + CurrDest.distance + " Link = (" + CurrDest.nextNode + ")");
		}
	}
	
	public void updateVector(String neighbor, Vector<node> newVector, Double weight) {
		if (this.weightCopy.containsKey(neighbor))
			return;
		this.managerCallback.onNeighborUpdate(neighbor);
		distanceTable.put(neighbor, newVector);
		if (!this.weightVector.contains(neighbor))
			this.setWeight(neighbor, weight);
		else if (this.weightVector.get(neighbor).equals(Double.POSITIVE_INFINITY))
			this.setWeight(neighbor, weight);
		else if (this.weightVector.contains(neighbor)) {
			if (this.weightVector.get(neighbor).equals(Double.POSITIVE_INFINITY))
				this.setWeight(neighbor, weight);
		}
	}
	
	public void saveNeighbor(String neighbor) {
		Double saveWeight;
		if (this.weightVector.containsKey(neighbor)) {
			saveWeight = this.weightVector.get(neighbor);
			this.weightCopy.put(neighbor, saveWeight);
		}
	}
	
	public void restoreNeighbor(String neighbor) {
		Double restoreWeight;
		if (this.weightCopy.containsKey(neighbor)) {
			restoreWeight = this.weightCopy.get(neighbor);
			this.weightCopy.remove(neighbor);
			this.setWeight(neighbor, restoreWeight);
		}
	}
	
	public void shutdownNeighbor(String neighbor, boolean isKill) {
		if (isKill && this.weightVector.containsKey(neighbor))
			this.weightVector.put(neighbor, Double.POSITIVE_INFINITY);
		else if (!isKill&& this.weightVector.containsKey(neighbor))
			this.weightVector.remove(neighbor);
		if (this.distanceTable.containsKey(neighbor))
			this.distanceTable.remove(neighbor);
	}
	
	private boolean isDeadNeighbor(String neighbor) {
		if (this.killonce_list.contains(neighbor))
			return true;
		if (this.weightVector.containsKey(neighbor)) {
			return this.weightVector.get(neighbor).equals(Double.POSITIVE_INFINITY);
		}
		return false;
	}
	
	private void clear_once_list() {
		this.killonce_list.clear();
	}
	
	public void add_kill_list(String nodeAddr) {
		this.killonce_list.add(nodeAddr);
	}
	
	public void upDateTable() {
		resetSelfVec();
		for (Iterator iter = distanceTable.keySet().iterator(); iter.hasNext();) {
			String neighbor = (String)iter.next();
			Vector<node> neighborVector = distanceTable.get(neighbor);
			for (int i = 0; i < neighborVector.size(); i++) {
				String destination = neighborVector.get(i).destination;
				if (isDeadNeighbor(destination)) {
					continue;
				}
				double D_neighbor_dest = neighborVector.get(i).distance + weightVector.get(neighbor);
				if (!localVector.containsKey(destination)) {
					node newDest = new node(D_neighbor_dest, neighbor, destination);
					localVector.put(destination, newDest);
				} else {
					node currDest = localVector.get(destination);
					if (D_neighbor_dest < currDest.distance) {
						currDest.nextNode = neighbor;
						currDest.distance = D_neighbor_dest;
					}
				}
				if (localVector.get(destination).distance > this.ThresHold)
					localVector.remove(destination);
			}
		}
		this.clear_once_list();
	}
	
	private String composeSingleUpdate(String neighbor) {
		String updateStr = "" + this.weightVector.get(neighbor) + NetLayer.fieldSpliter;
		for (Iterator iter = this.localVector.keySet().iterator(); iter.hasNext();) {
			String currDest = (String)iter.next();
			node currNode = this.localVector.get(currDest);
			if (!currNode.nextNode.equals(neighbor)) {
				updateStr = updateStr + currNode.destination + NetLayer.nodeSpliter + currNode.distance + NetLayer.listSpliter;
			}
		}
		return updateStr;
	}
	
	public void composeAllUpdates() {
		for (Iterator iter = this.weightVector.keySet().iterator(); iter.hasNext();) {
			String neighbor = (String)iter.next();
			if (this.isDeadNeighbor(neighbor))
				continue;
			//System.out.println("neighbor:" + neighbor + " " + this.weightVector.get(neighbor));
			this.managerCallback.onComposeAllUpdates(neighbor, this.composeSingleUpdate(neighbor));
		}
	}
	
	public void broadcastReachable(String content) {
		for (Iterator iter = this.localVector.keySet().iterator(); iter.hasNext();) {
			String neighbor = (String)iter.next();
			node currNode = this.localVector.get(neighbor);
			this.managerCallback.onBroadcastRechable(currNode.destination, currNode.nextNode, content);
		}
	}
	
	public String getNextHope(String nodeAddr) {
		String nextAddr = "";
		if (this.localVector.containsKey(nodeAddr)) {
			nextAddr = this.localVector.get(nodeAddr).nextNode;
		}
		return nextAddr;
	}
}
