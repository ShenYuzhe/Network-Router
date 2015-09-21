package applicationLayer;

import java.io.File;

import transportLayer.TransLayer;

public class AppLayer implements ApplicationCallback{
	private CommandReader commandReader;
	private Thread commandThread;
	public TransLayer transportManager;
	private static String defaultPath = "./";
	
	public AppLayer(String configFile) {
		// TODO Auto-generated constructor stub
		transportManager = new TransLayer(configFile, AppLayer.this);
		commandReader = new CommandReader(AppLayer.this);
		commandThread = new Thread(commandReader);
		commandThread.start();
	}
	
	public AppLayer() {
		// TODO Auto-generated constructor stub
		transportManager = new TransLayer(AppLayer.this);
		commandReader = new CommandReader(AppLayer.this);
		commandThread = new Thread(commandReader);
		commandThread.start();
	}
	
	public void onReceivePacket(String Packet) {}
	
	public void doHandleAction(ApplicationAction action) {
		if (action.operation.equals("LINKDOWN"))
			this.transportManager.networkManager.linkdown(action.destAddr);
		else if (action.operation.equals("LINKUP"))
			this.transportManager.networkManager.linkup(action.destAddr);
		else if (action.operation.equals("SHOWRT"))
			this.transportManager.networkManager.showrt();
		else if (action.operation.equals("CHANGECOST"))
			this.transportManager.networkManager.changeCost(action.destAddr, action.cost);
		else if (action.operation.equals("CLOSE")) {
			this.transportManager.networkManager.closeHost();
			this.transportManager.closeHost();
			this.commandReader.exitFlag = true;
		} else if (action.operation.equals("TRANSFER")) {
			File file = new File(AppLayer.defaultPath + action.fileName);
			if (!file.exists()) {
				System.out.println("file not exists");
				return;
			}
			if (!this.transportManager.networkManager.isValidDestination(action.destAddr)) {
				System.out.println("destnition not exists\nthe network might not be stablized yet.\nPlease try later");
				return;
			}
			this.transportManager.rdtSendFile(action.destAddr, file);
		} else if (action.operation.equals("ADDPROXY")) {
			this.transportManager.networkManager.addProxy(action.destAddr, action.proxyAddr);
		} else if (action.operation.equals("REMOVEPROXY")) {
			this.transportManager.networkManager.removeProxy(action.destAddr);
		} else if (action.operation.equals("SETTHRESHOLD")) {
			this.transportManager.networkManager.setThresHold(action.thresHold);
		}
	}

	@Override
	public void onReceiveCommand(ApplicationAction action) {
		// TODO Auto-generated method stub
		this.doHandleAction(action);
	}

}
