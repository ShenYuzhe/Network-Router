package networkLayer;

public interface NetworkCallback {

	
	public void onPacketReceive(byte[] packet);
	
	public void onVectorUpdate();
	
	public void onNeighborUpdate(String neighbor);
	
	public void onNeighborExpire(String neighbor);

	public void onComposeAllUpdates(String neighbor, String updateStr);
	
	public void onBroadcastRechable(String nodeAddr, String nextAddr, String content);
}
