package applicationLayer;

public class ApplicationAction {
	public String operation;
	public String destAddr;
	public double cost;
	public boolean isValid;
	public String erroMsg;
	public String fileName;
	public String proxyAddr;
	public int thresHold;
	
	public ApplicationAction() {
		// TODO Auto-generated constructor stub
		isValid = true;
		erroMsg = "";
		fileName = "";
		thresHold = 200;
	}

}
