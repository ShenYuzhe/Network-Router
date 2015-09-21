package networkLayer;

import hostManager.MainHost;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

import byteUtil.ByteHandler;
import transportLayer.TransportCallback;

public class NetLayer implements NetworkCallback{
	private TimeManager timeScanner;
	private DistanceManager Bellman_Ford;
	private PacketReceiver packetReceiver;
	private String pathName = "./config.txt";
	
	private TransportCallback transCallback;
	public final static String fieldSpliter = ";";
	public final static String nodeSpliter = "&";
	public final static String listSpliter = ",";
	public final static String headerSpliter = "#";
	
	public final static int packetLen = 1152;
	public final static int headerLen = 128;
	public final static int checksumLen = 2;
	
	/*******************************************************************************
	Following is the proxy map. In reality this should really be maintained in
	PacketReceiver so that every packet sent is going through proxy map. In my code,
	I do not want routing messages as "route_update" to go through proxy, so I implement
	it here so that only user data sent or forwarded will go through proxy if there
	is any. 
	*******************************************************************************/
	private Hashtable<String, String> proxyTable;
	
	
	/*******************************************************************************
	Following is the API offered to transport layer to transfer file
	*******************************************************************************/
	public int udtSend(String destination, byte[] content, boolean isAck) {
		NetworkHeader header = null;
		if (!isAck)
			header = this.composeHeader(destination, "user_data");
		else
			header = this.composeHeader(destination, "user_ack");
		byte[] packet = this.composePacket(header, content);
		String nextHop = this.Bellman_Ford.getNextHope(destination);
		if (nextHop.equals("")) {
			return -1;
		}
		if (this.proxyTable.containsKey(nextHop))
			nextHop = proxyTable.get(nextHop);

		this.packetReceiver.sendPackage(nextHop, packet);
		return 0;
	}
	
	/*******************************************************************************
	Following are APIs offered to Application Layer to configure routing information
	*******************************************************************************/
	public void showrt() {
		this.Bellman_Ford.printlocalVector();
	}
	
	public void linkdown(String neighbor) {
		if (!this.Bellman_Ford.isNeighborValidt(neighbor)) {
			System.out.println("invalid neighbor");
			return;
		}
		this.doLinkdown(neighbor);
		this.timeScanner.removeEvent(neighbor);
		NetworkHeader header = this.composeHeader(neighbor, "link_down");
		this.packetReceiver.sendPackage(neighbor, this.composePacket(header, "null".getBytes()));
	}
	
	public void linkup(String neighbor) {
		if (!this.Bellman_Ford.isOriginalNeighbor(neighbor)) {
			System.out.println("original link not exists");
			return;
		}
		this.doLinkup(neighbor);
		NetworkHeader header = this.composeHeader(neighbor, "link_up");
		this.packetReceiver.sendPackage(neighbor, this.composePacket(header, "null".getBytes()));
	}
	
	public void changeCost(String neighbor, Double cost) {
		if (!this.Bellman_Ford.isNeighborValidt(neighbor)) {
			System.out.println("invalid neighbor");
			return;
		}
		this.doChangeCost(neighbor, cost);
		NetworkHeader header = this.composeHeader(neighbor, "cost_change");
		String content = "" + cost + NetLayer.fieldSpliter;
		this.packetReceiver.sendPackage(neighbor, this.composePacket(header, content.getBytes()));
	}
	
	public void closeHost() {
		this.packetReceiver.exitFlag = true;
		this.timeScanner.exitFlag = true;
		this.packetReceiver.sendPackage(this.packetReceiver.getLocalAddr(),"close".getBytes());
	}
	
	public void addProxy(String neighborAddr, String proxyAddr) {
		if (!this.Bellman_Ford.isNeighborValidt(neighborAddr)) {
			System.out.println("invalid neighbor");
			return;
		}
		this.proxyTable.put(neighborAddr, proxyAddr);
	}
	
	public void removeProxy(String neighborAddr) {
		if (this.proxyTable.containsKey(neighborAddr))
			this.proxyTable.remove(neighborAddr);
		else
			System.out.println("proxy not exist");
	}
	
	public boolean isValidDestination(String destination) {
		return this.Bellman_Ford.isDestValid(destination);
	}
	
	public void setThresHold (int thresHold) {
		this.Bellman_Ford.setThresHold(thresHold);
	}
	/*******************************************************************************
	Following functions are internal functions within Network Layer
	*******************************************************************************/
	/*initial the weight*/
	private void initConfig() {
		try {
			File filename = new File(pathName);
			InputStreamReader reader = new InputStreamReader(
					new FileInputStream(filename));
			BufferedReader bufReader = new BufferedReader(reader);
			String line = "";
			int count = 0;
			while (line != null) {
				line = bufReader.readLine();
				if (line == null)
					break;
				count++;
				if (count == 1) {
					int listenPort, updatePeriod;
					String[] firstLine = line.split(" ");
					listenPort = Integer.parseInt(firstLine[0]);
					updatePeriod = Integer.parseInt(firstLine[1]);
					this.packetReceiver.setPort(listenPort);
					this.timeScanner.setUpdatePeriod(updatePeriod);
					this.Bellman_Ford.setLocalAddr(this.packetReceiver.getLocalAddr());
					continue;
				}
				String[] weightParser = line.split(" ");
				Vector<node> initialVector = new Vector<node>();
				node initialNode = new node(0.0, weightParser[0], weightParser[0]);
				initialVector.add(initialNode);
				this.Bellman_Ford.updateVector(weightParser[0], initialVector, Double.parseDouble(weightParser[1]));
			}
			bufReader.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public NetLayer(TransportCallback newCallback) {
		// TODO Auto-generated constructor stub
		this.transCallback = newCallback;
		Bellman_Ford = new DistanceManager(this);
		timeScanner = new TimeManager(this);
		packetReceiver = new PacketReceiver(NetLayer.this);
		initConfig();
		this.proxyTable = new Hashtable<String, String>();
		NetworkEvent event = new NetworkEvent();
		event.hostName = this.packetReceiver.getLocalAddr();
		event.createTime = System.currentTimeMillis();
		event.operation = "UPDATE";
		this.timeScanner.insertEvent(event);
		Thread timeThread = new Thread(timeScanner);
		Thread listenThread = new Thread(packetReceiver);
		listenThread.start();
		timeThread.start();
	}
	
	public NetLayer(String configFile, TransportCallback newCallback) {
		// TODO Auto-generated constructor stub
		this.pathName = "./" + configFile;
		this.transCallback = newCallback;
		Bellman_Ford = new DistanceManager(this);
		timeScanner = new TimeManager(this);
		packetReceiver = new PacketReceiver(NetLayer.this);
		initConfig();
		this.proxyTable = new Hashtable<String, String>();
		NetworkEvent event = new NetworkEvent();
		event.hostName = this.packetReceiver.getLocalAddr();
		event.createTime = System.currentTimeMillis();
		event.operation = "UPDATE";
		this.timeScanner.insertEvent(event);
		Thread timeThread = new Thread(timeScanner);
		Thread listenThread = new Thread(packetReceiver);
		listenThread.start();
		timeThread.start();
	}
	
	private NetworkHeader resolveHeader(byte[] headerBytes) {
		String rawHeaderStr = new String(headerBytes, 0, headerBytes.length);
		String headerStr = rawHeaderStr.split(NetLayer.headerSpliter)[0];
		String[] headerParser = headerStr.split(NetLayer.fieldSpliter);
		NetworkHeader header = new NetworkHeader();
		header.type = headerParser[0];
		header.srcAddr = headerParser[1];
		header.destAddr = headerParser[2];
		return header;
	}
	
	private NetworkHeader composeHeader(String destination, String type) {
		NetworkHeader header = new NetworkHeader();
		header.type = type;
		header.destAddr = destination;
		header.srcAddr = this.packetReceiver.getLocalAddr();
		return header;
	}
	
	private Double resolveWeight(String content) {
		Double weight = Double.parseDouble(content.split(NetLayer.fieldSpliter)[0]);
		return weight;
	}
 	
	private Vector<node> resolveNeighborVec(String content) {
		String neighborList = content.split(NetLayer.fieldSpliter)[1];
		String[] vecParser = neighborList.split(NetLayer.listSpliter);
		Vector<node> resVec = new Vector<node>();
		for (int i = 0; i < vecParser.length; i++) {
			String[] nodeParser = vecParser[i].split(NetLayer.nodeSpliter);
			resVec.add(new node(Double.parseDouble(nodeParser[1]), "", nodeParser[0]));
		}
		return resVec;
	}
	
	private Double resolveCostChange(String costStr) {
		return Double.parseDouble(costStr.split(NetLayer.fieldSpliter)[0]);
	}
	
	private byte[] composePacket(NetworkHeader header, byte[] content) {
		String headerStr = this.header2Str(header);
		byte[] headerBytes = headerStr.getBytes();
		byte[] headerChecksum = new byte[NetLayer.checksumLen];
		ByteHandler.calChecksum(headerChecksum, headerBytes);
		int packetLen = NetLayer.checksumLen + NetLayer.headerLen + content.length;
		byte[] packet = new byte[packetLen];
		System.arraycopy(headerChecksum, 0, packet, 0, NetLayer.checksumLen);
		System.arraycopy(headerBytes, 0, packet, NetLayer.checksumLen, headerBytes.length);
		System.arraycopy(content, 0, packet, NetLayer.checksumLen + NetLayer.headerLen, content.length);
		return packet;
	}
	
	private void printPacketReceive(NetworkHeader header) {
		if (header.type.equals("user_data")) {
			System.out.println("Packet received");
			System.out.println("Source = " + header.srcAddr);
			System.out.println("Destination = " + header.destAddr);
		}
	}

	private void distrbutePacket(NetworkHeader header, byte[] content) {
		this.printPacketReceive(header);
		if (header.destAddr.equals(this.packetReceiver.getLocalAddr())) {
			if (header.type.equals("user_data") || header.type.equals("user_ack")) {
				//MainHost.applicationManager.transportManager.onReceivePacket(header.srcAddr, content);
				this.transCallback.onReceivePacket(header.srcAddr, content);
				return;
			}
			String[] contentParser = new String(content, 0, content.length).split(NetLayer.headerSpliter);
			String networkContent = contentParser[0];
			if (header.type.equals("cost_change")) {
				Double weight = resolveCostChange(networkContent);
				this.Bellman_Ford.setWeight(header.srcAddr, weight);
			} else if (header.type.equals("route_update")) {
				Vector<node> neighborVec = resolveNeighborVec(networkContent);
				Double weight = this.resolveWeight(networkContent);
				this.Bellman_Ford.updateVector(header.srcAddr, neighborVec, weight);
				//this.onNeighborUpdate(header.srcAddr);
			} else if (header.type.equals("link_down")) {
				this.doLinkdown(header.srcAddr);
			} else if (header.type.equals("link_up")) {
				this.doLinkup(header.srcAddr);
			} else if (header.type.equals("dead_node")) {
				this.Bellman_Ford.add_kill_list(networkContent);
			}
		} else {
			String nextAddr = this.Bellman_Ford.getNextHope(header.destAddr);
			if (header.type.equals("user_data") 
					&& this.proxyTable.containsKey(nextAddr))
				nextAddr = proxyTable.get(nextAddr);
			this.packetReceiver.sendPackage(nextAddr,
					this.composePacket(header, content)); //forward a packet
		}
	}
	
	public String header2Str(NetworkHeader header) {
		String headerStr = "";
		headerStr = headerStr + header.type + NetLayer.fieldSpliter 
				+ header.srcAddr + NetLayer.fieldSpliter + header.destAddr + NetLayer.headerSpliter;
		return headerStr;
	}

	@Override
	public void onPacketReceive(byte[] packet) {
		// TODO Auto-generated method stub
		if (ByteHandler.isCorrupt(packet, 0, NetLayer.checksumLen + NetLayer.headerLen,
			NetLayer.checksumLen)) { // drop all the packets whose
			return;													// Network Layer header is corrupted
		}
		byte[] headerBytes = new byte[NetLayer.headerLen];
		System.arraycopy(packet, checksumLen, headerBytes, 0, NetLayer.headerLen);
		NetworkHeader header = this.resolveHeader(headerBytes);
		int headerCheckLen = NetLayer.headerLen + NetLayer.checksumLen;
		byte[] contentBytes = new byte[packet.length - headerCheckLen];
		System.arraycopy(packet, headerCheckLen, contentBytes, 0, contentBytes.length);
		this.distrbutePacket(header, contentBytes);
	}

	@Override
	public void onVectorUpdate() {
		// TODO Auto-generated method stub
		NetworkEvent event = new NetworkEvent();
		event.hostName = this.packetReceiver.getLocalAddr();
		event.createTime = System.currentTimeMillis();
		event.operation = "UPDATE";
		this.timeScanner.insertEvent(event);
		this.Bellman_Ford.upDateTable();
		//this.Bellman_Ford.printlocalVector();
		this.Bellman_Ford.composeAllUpdates();
	}

	@Override
	public void onNeighborExpire(String neighbor) {
		// TODO Auto-generated method stub
		this.Bellman_Ford.shutdownNeighbor(neighbor, true);
		this.timeScanner.removeEvent(neighbor);
		this.Bellman_Ford.broadcastReachable(neighbor);
	}
	
	@Override
	public void onComposeAllUpdates(String neighbor, String updateStr) {
		// TODO Auto-generated method stub
		NetworkHeader updateHeader = new NetworkHeader();
		updateHeader.destAddr = neighbor;
		updateHeader.srcAddr = this.packetReceiver.getLocalAddr();
		updateHeader.type = "route_update";
		this.packetReceiver.sendPackage(neighbor,
				this.composePacket(updateHeader, (updateStr + NetLayer.headerSpliter).getBytes()));
	}
	
	private void doLinkup(String neighbor) { //only deal with the data stored in Vector Distance Class
		this.Bellman_Ford.restoreNeighbor(neighbor);
	}

	private void doLinkdown(String neighbor) { //only deal with the data stored in Vector Distance Class
		this.Bellman_Ford.saveNeighbor(neighbor);
		this.Bellman_Ford.shutdownNeighbor(neighbor, false);
	}
	
	private void doChangeCost(String neighbor, Double cost) {
		this.Bellman_Ford.setWeight(neighbor, cost);
	}

	@Override
	public void onNeighborUpdate(String neighbor) {
		// TODO Auto-generated method stub
		NetworkEvent event = new NetworkEvent();
		event.hostName = neighbor;
		event.createTime = System.currentTimeMillis();
		event.operation = "EXPIRE";
		this.timeScanner.insertEvent(event);
	}

	@Override
	public void onBroadcastRechable(String nodeAddr, String nextAddr, String content) {
		// TODO Auto-generated method stub
		NetworkHeader header = this.composeHeader(nodeAddr, "dead_node");
		this.packetReceiver.sendPackage(nextAddr,
				this.composePacket(header, (content + NetLayer.fieldSpliter).getBytes()));
	}
}
