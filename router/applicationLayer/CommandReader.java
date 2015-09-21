package applicationLayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandReader implements Runnable {
	
	public boolean exitFlag;
	private ApplicationCallback AppManager;

	public CommandReader(ApplicationCallback newCallback) {
		// TODO Auto-generated constructor stub
		exitFlag = false;
		this.AppManager = newCallback;
	}
	
	private ApplicationAction commandAnalyzer(String command) {
		ApplicationAction action = new ApplicationAction();
		String[] commandParser = command.split(" ");
		action.operation = commandParser[0];
		if (action.operation.equals("LINKDOWN")) {
			if (commandParser.length != 3) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else
				action.destAddr = commandParser[1] + ":" +  commandParser[2];
		} else if (action.operation.equals("LINKUP")) {
			if (commandParser.length != 3) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else
				action.destAddr = commandParser[1] + ":" +  commandParser[2];
		} else if (action.operation.equals("CHANGECOST")) {
			if (commandParser.length != 4) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else {
				action.destAddr = commandParser[1] + ":" +  commandParser[2];
				Double cost = Double.parseDouble(commandParser[3]);
				if (cost <= 0) {
					action.isValid = false;
					action.erroMsg = "cost between links should be positive";
				} else
					action.cost = cost;
			}
		} else if (action.operation.equals("SHOWRT")) {
		} else if (action.operation.equals("CLOSE")) {
		} else if (action.operation.equals("TRANSFER")) {
			if (commandParser.length != 4) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else {
				action.destAddr = commandParser[2] + ":" +  commandParser[3];
				action.fileName = commandParser[1];
			}
		} else if (action.operation.equals("ADDPROXY")) {
			if (commandParser.length != 5) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else {
				action.proxyAddr = commandParser[1] + ":" +  commandParser[2];
				action.destAddr = commandParser[3] + ":" + commandParser[4];
			}
		} else if (action.operation.equals("REMOVEPROXY")) {
			if (commandParser.length != 3) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else {
				action.destAddr = commandParser[1] + ":" + commandParser[2];
			}
		} else if (action.operation.equals("SETTHRESHOLD")){
			if (commandParser.length != 2) {
				action.erroMsg = "in valid number of parameters";
				action.isValid = false;
			} else {
				action.thresHold = Integer.parseInt(commandParser[1]);
			}
		} else {
			action.isValid = false;
			action.erroMsg = "unknown Operation";
		}
		return action;
	}

	private void actionHandler(ApplicationAction action) {
		if (!action.isValid) {
			System.out.println("error: " + action.erroMsg);
			return;
		}
		this.AppManager.onReceiveCommand(action);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (!exitFlag) {
			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
			String message = "";
			System.out.print(">");
			try {
				message = input.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ApplicationAction action = this.commandAnalyzer(message);
			this.actionHandler(action);
			//System.out.println(message);
		}
	}

}
