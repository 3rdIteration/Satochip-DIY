package dev.satochip.tests.satodime;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SatodimeLabelTest extends ReadySatodimeTest {

    @Test
    void setAndGetLabel() {
        byte[] label = "satodime".getBytes();
        byte[] data = new byte[1 + label.length];
        data[0] = (byte) label.length;
        System.arraycopy(label, 0, data, 1, label.length);
        sc.send(0x3D, 0x00, 0x00, data).requireSuccess();

        Response r = sc.send(0x3D, 0x00, 0x01).requireSuccess();
        assertEquals(label.length, r.data[0] & 0xFF);
        assertArrayEquals(label, Hex.slice(r.data, 1, label.length));
    }

    @Test
    void resetLabel() {
        sc.send(0x3D, 0x00, 0x00, new byte[]{0x01, 0x42}).requireSuccess();
        sc.send(0x3D, 0x00, 0x00, new byte[0]).requireSuccess();
        Response r = sc.send(0x3D, 0x00, 0x01).requireSuccess();
        assertEquals(1, r.data.length);
        assertEquals(0, r.data[0] & 0xFF);
    }

    @Test
    void tooLongLabelRejected() {
        byte[] data = new byte[66];
        data[0] = 65;
        assertEquals(SW_INVALID_PARAMETER, sc.send(0x3D, 0x00, 0x00, data).sw);
    }
}
