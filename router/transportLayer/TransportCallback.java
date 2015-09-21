package transportLayer;

public interface TransportCallback {
	public void onReceivePacket(String srcAddr, byte[] packet);
	
	public void onTimeOut(TransportHeader header);
}
