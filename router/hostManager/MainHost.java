package hostManager;

import applicationLayer.AppLayer;



public class MainHost {
	public static AppLayer applicationManager;

	public MainHost() {
		// TODO Auto-generated constructor stub
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length >= 1) {
			String configFile = args[0];
			applicationManager = new AppLayer(configFile);
		} else {
			System.out.println("config file not specified, default config file is used");
			applicationManager = new AppLayer();
		}
	}

}
