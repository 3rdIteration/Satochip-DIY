package dev.satochip.tests.common;

import java.util.Arrays;

/**
 * A parsed APDU response: returned data + status word.
 */
public final class Response {

    public final byte[] data;
    public final int sw;

    public Response(byte[] data, int sw) {
        this.data = data == null ? new byte[0] : data;
        this.sw = sw;
    }

    public static Response parse(byte[] raw) {
        return new Response(Hex.data(raw), Hex.sw(raw));
    }

    public boolean isSuccess() {
        return sw == 0x9000;
    }

    public Response require(int expectedSw) {
        if (sw != expectedSw) {
            throw new AssertionError(String.format(
                    "Expected SW %04X but got %04X (data=%s)", expectedSw, sw, Hex.encode(data)));
        }
        return this;
    }

    public Response requireSuccess() {
        return require(0x9000);
    }

    public byte[] data() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("Response[sw=%04X, data=%s]", sw, Hex.encode(data));
    }

    public boolean dataEquals(byte[] other) {
        return Arrays.equals(data, other);
    }
}
