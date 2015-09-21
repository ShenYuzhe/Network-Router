package networkLayer;

import hostManager.MainHost;

import java.io.*;
import java.net.*;

public class PacketReceiver implements Runnable{
	private String localIP;
	private int localPort;
	final private int MSS = 1500;
	private NetworkCallback managerCallback;
	private DatagramSocket server;
	public boolean exitFlag;
	
	public PacketReceiver(NetworkCallback newCallback) {
		// TODO Auto-generated constructor stub
		exitFlag = false;
		try {
			localIP = InetAddress.getLocalHost().toString().split("/")[1];
		} catch (UnknownHostException e) {}
		managerCallback = newCallback;
	}
	
	public void setPort(int newPort) {
		this.localPort = newPort;
	}
	
	public String getLocalAddr() {
		return (localIP + ":" + localPort);
	}
	
	public void sendPackage(String address, byte[] sendBuf) {
		String[] addrParser = address.split(":");
		String Ip = addrParser[0];
		int port = Integer.parseInt(addrParser[1]);
		//System.out.println(packetStr);
		try{
			DatagramSocket sender = new DatagramSocket();
			InetAddress addr = InetAddress.getByName(Ip);
			DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, addr, port);
			sender.send(sendPacket);
			sender.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			server = new DatagramSocket(this.localPort);
		} catch (Exception e) {
			System.out.println(e);
		}
		while (!this.exitFlag) {
			try {
				byte[] recvBuf = new byte[MSS];
				DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
				this.server.receive(recvPacket);
				//String recvStr = new String(recvPacket.getData(), 0, recvPacket.getLength());
				this.managerCallback.onPacketReceive(recvBuf);
			} catch (Exception e) {}
		}
	}

}
