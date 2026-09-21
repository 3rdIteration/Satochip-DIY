package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Apdu;
import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SatochipStatusTest extends AbstractSatochipTest {

    @Test
    void statusLayoutBeforeSetup() {
        Response r = status().requireSuccess();
        assertEquals(12, r.data.length);
        assertEquals(0, r.data[0], "protocol major");
        assertEquals(12, r.data[1], "protocol minor");
        assertEquals(0, r.data[2], "applet major");
        assertEquals(6, r.data[3], "applet minor");
        assertEquals(0, r.data[10], "setupDone");
        assertEquals(1, r.data[11], "needs_secure_channel");
    }

    @Test
    void statusReflectsSetup() {
        setupDefault().requireSuccess();
        Response r = status().requireSuccess();
        assertEquals(1, r.data[10], "setupDone after setup");
        assertEquals(0, r.data[9], "not seeded");
        assertEquals(0, r.data[8], "no 2FA");
    }

    @Test
    void wrongP1AndP2() {
        assertEquals(SW_INCORRECT_P1, card.send(0x3C, 0x01, 0x00).sw);
        assertEquals(SW_INCORRECT_P2, card.send(0x3C, 0x00, 0x01).sw);
    }

    @Test
    void spuriousSelectForUnknownAidIsRejected() {
        byte[] unknownAid = Hex.decode("A0A1A2A3A4");
        Response r = card.transmit(Apdu.case3(0x00, 0xA4, 0x04, 0x00, unknownAid));
        assertEquals(SW_FILE_NOT_FOUND, r.sw);
    }
}
