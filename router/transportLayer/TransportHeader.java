package transportLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class TransportHeader {
	public String type;
	public String fileName;
	public boolean isEnd;
	public long seq;
	public long length;
	public long fileSize;
	public byte[] checksum;
	public long sentTime;
	public String destination;
	
	private FileInputStream ifileStream = null;
	private byte[] tempBuf;

	public TransportHeader(File file) {
		// TODO Auto-generated constructor stub
		this.seq = 0;
		this.type = "";
		this.fileName = "";
		this.fileSize = 0;
		this.isEnd = true;
		this.length = 0;
		this.checksum = new byte[0];
		this.destination = "";
		if (file != null) {
			try {
				this.ifileStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {}
		}
		this.tempBuf = new byte[TransLayer.MSS];
	}
	
	public void setCheckSum(byte[] newChecksum) {
		this.checksum = newChecksum;
	}
	
	public void updateSeq(long newSeq) {
		this.seq = newSeq;
	}
	
	public void setTimer() {
		this.sentTime = System.currentTimeMillis();
	}
	
	public void readSegment(byte[] readBuf, int idx, int segLen) {
		try {
			ifileStream.read(readBuf, (int) 0, segLen);
			System.arraycopy(readBuf, 0, this.tempBuf, 0, readBuf.length); // save a copy of the current buf
		} catch (Exception e) {}
	}
	
	public void getCurrentCopy(byte[] targetBuf) {
		System.arraycopy(this.tempBuf, 0, targetBuf, 0, targetBuf.length);
	}
	
	public void endRead() {
		try {
			this.ifileStream.close();
		} catch (Exception e) {}
	}
}
