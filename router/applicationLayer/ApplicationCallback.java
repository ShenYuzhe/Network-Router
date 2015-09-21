package applicationLayer;

public interface ApplicationCallback {
	public void onReceivePacket(String packet);
	public void onReceiveCommand(ApplicationAction action);
}
