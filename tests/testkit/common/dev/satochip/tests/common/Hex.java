package dev.satochip.tests.common;

public final class Hex {

    private Hex() {
    }

    public static byte[] decode(String hex) {
        if (hex == null) {
            return null;
        }
        String s = hex.replaceAll("\\s", "");
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException("Odd-length hex string: " + hex);
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }

    public static String encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    public static String encode(byte[] data, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", data[off + i] & 0xFF));
        }
        return sb.toString();
    }

    public static int sw(byte[] response) {
        if (response == null || response.length < 2) {
            throw new IllegalArgumentException("Response too short");
        }
        return ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
    }

    public static byte[] data(byte[] response) {
        if (response == null || response.length < 2) {
            return new byte[0];
        }
        byte[] out = new byte[response.length - 2];
        System.arraycopy(response, 0, out, 0, out.length);
        return out;
    }

    public static byte[] slice(byte[] src, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(src, off, out, 0, len);
        return out;
    }

    public static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] out = new byte[len];
        int off = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, off, p.length);
            off += p.length;
        }
        return out;
    }

    public static int getShort(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    public static byte[] setShort(int value) {
        return new byte[]{(byte) (value >> 8), (byte) value};
    }

    public static byte[] padLeft(byte[] value, int size) {
        if (value.length == size) {
            return value;
        }
        if (value.length > size) {
            return slice(value, value.length - size, size);
        }
        byte[] out = new byte[size];
        System.arraycopy(value, 0, out, size - value.length, value.length);
        return out;
    }
}
