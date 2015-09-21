package transportLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import applicationLayer.ApplicationCallback;
import networkLayer.NetLayer;
import networkLayer.NetworkHeader;
import byteUtil.ByteHandler;

public class TransLayer implements TransportCallback {
	public NetLayer networkManager;
	private ApplicationCallback AppCallback;
	private Hashtable<String, TransportHeader> windows;
	private Hashtable<String, CacheBlock> caches;
	private TimeoutChecker timeManager;
	
	public static int MSS = 1024;
	public static int headerLen = 128;
	public static int checksumLen = 2;
	
	public final static String fieldSpliter = ";";
	public final static String nodeSpliter = "&";
	public final static String listSpliter = ",";
	public final static String headerSpliter = "#";
	
	private String savePath = "./output/";
	
	/*******************************************************************************
	Following are the APIs offered to application Layer
	*******************************************************************************/
	
	public void rdtSendFile(String destination, File file) {
		TransportHeader header = new TransportHeader(file);
		header.destination = destination;
		header.fileName = file.getName();
		header.fileSize = file.length();
		header.type = "data";
		if ((int)header.seq + TransLayer.MSS < header.fileSize) {
			header.length = TransLayer.MSS;
			header.isEnd = false;
		} else {
			header.length = header.fileSize - header.seq;
			header.isEnd = true;
		}
		header.updateSeq(0);
		this.insertWindow(header);
		this.timeManager.insertEvent(header);
		this.doSendFile(header);
		header.setTimer();
	}
	
	public void closeHost() {
		this.timeManager.exitFlag = true;
	}
	
	/*******************************************************************************
	Above are the APIs offered to application Layer
	*******************************************************************************/
	
	private String composeHeaderStr(TransportHeader header) {
		String headerStr= "";
		headerStr = headerStr + header.type + TransLayer.fieldSpliter
				+ header.seq + TransLayer.fieldSpliter
				+ header.isEnd + TransLayer.fieldSpliter
				+ header.length + TransLayer.fieldSpliter 
				+ header.fileName + TransLayer.headerSpliter;
		return headerStr;
	}
	
	private void composePacket(byte[] target, byte[] headerBytes, byte[] content) {
		byte[] headerChecksum = new byte[TransLayer.checksumLen];
		ByteHandler.calChecksum(headerChecksum, headerBytes);
		byte[] dataChecksum = new byte[TransLayer.checksumLen];
		ByteHandler.calChecksum(dataChecksum, content);
		int headerIdx = TransLayer.checksumLen,
				dataChecksumIdx = TransLayer.checksumLen + TransLayer.headerLen,
				dataIdx = 2 * TransLayer.checksumLen + TransLayer.headerLen;
		System.arraycopy(headerChecksum, 0, target, 0, TransLayer.checksumLen);
		System.arraycopy(headerBytes, 0, target, headerIdx, headerBytes.length);
		System.arraycopy(dataChecksum, 0, target, dataChecksumIdx, TransLayer.checksumLen);
		System.arraycopy(content, 0, target, dataIdx, content.length);
	}
	
	private void insertWindow(TransportHeader header) {
		String key = header.destination + TransLayer.fieldSpliter + header.fileName;
		this.windows.put(key, header);
	}
	
	private void doSendFile (TransportHeader header) {
		byte[] readBuf = new byte[(int) header.length];
		header.readSegment(readBuf, 0, (int)header.length);
		this.doSendPacket(readBuf, header, false);
	}
	
	private void reSendSegment (TransportHeader header) {
		byte[] resendBuf = new byte[(int)header.length];
		header.getCurrentCopy(resendBuf);
		this.doSendPacket(resendBuf, header, false);
	}
	
	private void doSendPacket (byte[] content, TransportHeader header, boolean isAck) {
		try {
			String headerStr = composeHeaderStr(header);
			int packetLen = TransLayer.headerLen + content.length + 2 * TransLayer.checksumLen;
			byte[] packet = new byte[packetLen];
			this.composePacket(packet, headerStr.getBytes(), content);
			this.networkManager.udtSend(header.destination, packet, isAck);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public TransLayer(String configFile, ApplicationCallback newCallback) {
		// TODO Auto-generated constructor stub
		this.AppCallback = newCallback;
		this.networkManager = new NetLayer(configFile, TransLayer.this);
		this.timeManager = new TimeoutChecker(TransLayer.this);
		this.windows = new Hashtable<String, TransportHeader>();
		this.caches = new Hashtable<String, CacheBlock>();
		Thread timeThread = new Thread(timeManager);
		timeThread.start();
	}
	
	public TransLayer(ApplicationCallback newCallback) {
		// TODO Auto-generated constructor stub
		this.AppCallback = newCallback;
		this.networkManager = new NetLayer(TransLayer.this);
		this.timeManager = new TimeoutChecker(TransLayer.this);
		this.windows = new Hashtable<String, TransportHeader>();
		this.caches = new Hashtable<String, CacheBlock>();
		Thread timeThread = new Thread(timeManager);
		timeThread.start();
	}
	
	private TransportHeader resolveHeader(byte[] headerByte) {
		TransportHeader header = new TransportHeader(null);
		String rawHeaderStr = new String(headerByte, 0, headerByte.length);
		String headerStr = rawHeaderStr.split(TransLayer.headerSpliter)[0];
		String headerParser[] = headerStr.split(TransLayer.fieldSpliter);
		header.type = headerParser[0];
		header.seq = Long.parseLong(headerParser[1]);
		header.isEnd = Boolean.parseBoolean(headerParser[2]);
		header.length = Long.parseLong(headerParser[3]);
		header.fileName = headerParser[4];
		return header;
	}
	
	private void updateWindow(TransportHeader sendHeader) {
		String key = sendHeader.destination + TransLayer.fieldSpliter + sendHeader.fileName;
		if (sendHeader.isEnd)
			this.windows.remove(key);
		else {
			sendHeader.updateSeq(sendHeader.seq + sendHeader.length);
			if (sendHeader.seq + TransLayer.MSS < sendHeader.fileSize)
				sendHeader.length = TransLayer.MSS;
			else {
				sendHeader.length = sendHeader.fileSize - sendHeader.seq;
				sendHeader.isEnd = true;
			}
		}
	}
	
	private void doAck(String type, TransportHeader header) {
		TransportHeader AckHeader = new TransportHeader(null);
		AckHeader.type = type;
		AckHeader.seq = header.seq;
		AckHeader.length = header.length;
		AckHeader.isEnd = header.isEnd;
		AckHeader.fileName = header.fileName;
		AckHeader.destination = header.destination;
		this.doSendPacket("null".getBytes(), AckHeader, true);
	}
	
	private void onReceiveACK(TransportHeader header) {
		String key = header.destination + TransLayer.fieldSpliter + header.fileName;
		if (this.windows.containsKey(key)) {
			TransportHeader storeHeader = this.windows.get(key);
			if (storeHeader.seq != header.seq)   // ignore ack for old packets
				return;
			this.updateWindow(storeHeader);
			if (!header.isEnd) {
				this.doSendFile(storeHeader);
				storeHeader.setTimer();
			} else {
				this.timeManager.removeEvent(storeHeader);
				storeHeader.endRead();
				System.out.println("file sent successfully");
			}
		} 
	}
	
	private void onReceiveNAK(TransportHeader header) {
		System.out.println("receive NAK");
		String key = header.destination + TransLayer.fieldSpliter + header.fileName;
		if (this.windows.containsKey(key)) {
			TransportHeader storeHeader = this.windows.get(key);
			if (storeHeader.seq != header.seq) {  // ignore ack for old packets
				System.out.println("not resend");
				return;
			}
			System.out.println("resend due to NAK");
			this.reSendSegment(storeHeader);
			storeHeader.setTimer();
		}
	}
	
	private void storeSegment(TransportHeader header, byte[] content) {
		CacheBlock currBlock;
		String key = header.destination + TransLayer.fieldSpliter + header.fileName;
		if (this.caches.containsKey(key))
			currBlock = caches.get(key);
		else {
			currBlock = new CacheBlock(this.savePath + header.fileName);
			this.caches.put(key, currBlock);
		}
		if (header.seq == currBlock.expectSeq) {
			if (ByteHandler.isCorrupt(content, 0, content.length, TransLayer.checksumLen)) {    // When the sequence number is expected
				this.doAck("nak", header);								// and the data is corrupted
				return;													// an NAK should be sent;
			}
			currBlock.writeSegment(content, TransLayer.checksumLen, (int)header.length);
			currBlock.expectSeq = header.seq + header.length;
		}
		if (header.isEnd) {
			currBlock.endWrite();
			caches.remove(key);
			System.out.println("File received successfully");
		}
		this.doAck("ack", header);
	}
	
	private void analyzePacket(TransportHeader header, byte[] content) {
		if (header.type.equals("ack")) {
			this.onReceiveACK(header);
		} else if (header.type.equals("nak")) {
			this.onReceiveNAK(header);
		} else if (header.type.equals("data")) {
			this.storeSegment(header, content);
		}
	}

	public void onReceivePacket(String destination, byte[] packet) {
		int headerCheckLen = TransLayer.checksumLen + TransLayer.headerLen;
		if (ByteHandler.isCorrupt(packet, 0, headerCheckLen, TransLayer.checksumLen)) {// ignore all the packets whose 
			System.out.println("corrupt");
			return;												// transport Layer header is corrupted 
		}
		byte[] headerBytes = new byte[TransLayer.headerLen];
		System.arraycopy(packet, TransLayer.checksumLen, headerBytes, 0, TransLayer.headerLen);
		TransportHeader header = this.resolveHeader(headerBytes);
		header.destination = destination;
		byte[] contentBytes = new byte[packet.length - headerCheckLen];
		System.arraycopy(packet, headerCheckLen, contentBytes, 0, contentBytes.length);
		analyzePacket(header, contentBytes);
	}

	@Override
	public void onTimeOut(TransportHeader header) {
		// TODO Auto-generated method stub
		this.reSendSegment(header);
		header.setTimer();
	}
}
