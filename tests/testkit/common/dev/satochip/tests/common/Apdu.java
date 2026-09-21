package dev.satochip.tests.common;

/**
 * Helpers to build ISO 7816 command APDUs in the short form used by these applets.
 */
public final class Apdu {

    private Apdu() {
    }

    /** Case 1: header only (no data, no Le). */
    public static byte[] case1(int cla, int ins, int p1, int p2) {
        return new byte[]{(byte) cla, (byte) ins, (byte) p1, (byte) p2};
    }

    /** Case 2: header + Le. */
    public static byte[] case2(int cla, int ins, int p1, int p2, int le) {
        return new byte[]{(byte) cla, (byte) ins, (byte) p1, (byte) p2, (byte) le};
    }

    /** Case 3: header + Lc + data. */
    public static byte[] case3(int cla, int ins, int p1, int p2, byte[] data) {
        if (data == null || data.length == 0) {
            return case1(cla, ins, p1, p2);
        }
        if (data.length > 255) {
            throw new IllegalArgumentException("Short APDU data must be <= 255 bytes");
        }
        byte[] out = new byte[5 + data.length];
        out[0] = (byte) cla;
        out[1] = (byte) ins;
        out[2] = (byte) p1;
        out[3] = (byte) p2;
        out[4] = (byte) data.length;
        System.arraycopy(data, 0, out, 5, data.length);
        return out;
    }

    /**
     * Raw command body used inside a secure-channel envelope. The Lc byte is always present,
     * even for empty data, because the applets read the inner Lc from the decrypted buffer.
     */
    public static byte[] command(int cla, int ins, int p1, int p2, byte[] data) {
        int dataLen = data == null ? 0 : data.length;
        if (dataLen > 255) {
            throw new IllegalArgumentException("Short APDU data must be <= 255 bytes");
        }
        byte[] out = new byte[5 + dataLen];
        out[0] = (byte) cla;
        out[1] = (byte) ins;
        out[2] = (byte) p1;
        out[3] = (byte) p2;
        out[4] = (byte) dataLen;
        if (dataLen > 0) {
            System.arraycopy(data, 0, out, 5, dataLen);
        }
        return out;
    }
}
