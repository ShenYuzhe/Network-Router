package transportLayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class CacheBlock {
	private FileOutputStream ofstream;
	public long expectSeq;
	public CacheBlock(String filePath) {
		// TODO Auto-generated constructor stub
		try {
			this.ofstream = new FileOutputStream(filePath);
		} catch (FileNotFoundException e) {}
		this.expectSeq = 0;
	}
	
	public void updateSeq(long newSeq) {
		this.expectSeq = newSeq;
	}
	
	public void writeSegment(byte[] buf, int idx, int length) {
		try {
			this.ofstream.write(buf, idx, length);
		} catch (Exception e) {}
	}
	
	public void endWrite() {
		try {
			this.ofstream.close();
		} catch (Exception e) {}
	}
	
}
