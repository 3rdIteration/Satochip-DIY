package dev.satochip.tests.satochip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SatochipPkiAuthTest extends AbstractSatochipTest {

    @Test
    void pkiCommandsRequirePin() {
        assertEquals(SW_UNAUTHORIZED, sc.send(0x98, 0x00, 0x00).sw);
        assertEquals(SW_UNAUTHORIZED, sc.send(0x94, 0x00, 0x00, new byte[32]).sw);
    }
}
