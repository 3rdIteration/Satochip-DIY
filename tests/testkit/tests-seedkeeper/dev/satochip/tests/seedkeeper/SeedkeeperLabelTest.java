package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedkeeperLabelTest extends ReadySeedkeeperTest {

    @Test
    void setAndGetLabel() {
        byte[] label = "seedkeeper".getBytes();
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
    void wrongP2Rejected() {
        assertEquals(SW_INCORRECT_P2, sc.send(0x3D, 0x00, 0x02, new byte[0]).sw);
    }
}
