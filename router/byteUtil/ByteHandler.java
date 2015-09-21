package byteUtil;
import java.nio.ByteBuffer;

import transportLayer.TransLayer;

public class ByteHandler {
	public static int addSum(int len, byte[] original, int start, int end) {
		int oriLen = original.length;
		int originIdx = start, sum = 0;
		int threshold = (1 << (len * Byte.SIZE));
		while (originIdx < end) {
			int temp = 0;
			temp = temp + ((0xff & original[originIdx]) << Byte.SIZE);
			if (originIdx <= oriLen - len)
				temp = temp + (0xff & original[originIdx + 1]);
			sum = sum + temp;
			if (sum >= threshold) {
				sum -= threshold;
				sum += 1;
			}
			originIdx += 2;
		}
		return sum;
	}
	
	public static int addSum(int len, byte[] original) {
		return addSum(len, original, 0, original.length);
	}
	
	public static void calChecksum(byte[] checksum, byte[] original) {
		int sum = addSum(checksum.length, original);
		ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
		buffer.putInt(~sum);
		checksum[0] = buffer.array()[2];
		checksum[1] = buffer.array()[3];
	}

	public static boolean isCorrupt(byte[] checkAdata, int start, int end, int checkLen) {
		return (ByteHandler.addSum(TransLayer.checksumLen,
				checkAdata, start, end) != (1 << (checkLen * Byte.SIZE)) - 1);
	}

}
