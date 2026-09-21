package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SatochipLabelTest extends ReadySatochipTest {

    @Test
    void setAndGetLabel() {
        byte[] label = "my-satochip".getBytes();
        byte[] setData = new byte[1 + label.length];
        setData[0] = (byte) label.length;
        System.arraycopy(label, 0, setData, 1, label.length);
        sc.send(0x3D, 0x00, 0x00, setData).requireSuccess();

        Response r = sc.send(0x3D, 0x00, 0x01).requireSuccess();
        assertEquals(label.length, r.data[0] & 0xFF);
        assertArrayEquals(label, Hex.slice(r.data, 1, label.length));
    }

    @Test
    void resetLabel() {
        sc.send(0x3D, 0x00, 0x00, new byte[]{0x01, 0x41}).requireSuccess();
        sc.send(0x3D, 0x00, 0x00, new byte[0]).requireSuccess();

        Response r = sc.send(0x3D, 0x00, 0x01).requireSuccess();
        assertEquals(1, r.data.length);
        assertEquals(0, r.data[0] & 0xFF);
    }

    @Test
    void tooLongLabelRejected() {
        byte[] label = new byte[65];
        byte[] setData = new byte[66];
        setData[0] = 65;
        System.arraycopy(label, 0, setData, 1, label.length);
        assertEquals(SW_INVALID_PARAMETER, sc.send(0x3D, 0x00, 0x00, setData).sw);
    }

    @Test
    void wrongP2Rejected() {
        assertEquals(SW_INCORRECT_P2, sc.send(0x3D, 0x00, 0x02, new byte[0]).sw);
    }
}
