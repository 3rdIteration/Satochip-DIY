package dev.satochip.tests.satodime;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatodimeStatusTest extends AbstractSatodimeTest {

    @Test
    void statusLayoutBeforeSetup() {
        Response r = card.send(0x3C, 0x00, 0x00).requireSuccess();
        assertEquals(12, r.data.length);
        assertEquals(0, r.data[10], "setupDone");
        assertEquals(1, r.data[11], "needs_secure_channel");
    }

    @Test
    void wrongP1AndP2() {
        assertEquals(SW_INCORRECT_P1, card.send(0x3C, 0x01, 0x00).sw);
        assertEquals(SW_INCORRECT_P2, card.send(0x3C, 0x00, 0x01).sw);
    }

    @Test
    void setupReturnsUnlockMaterial() {
        Response r = setup().requireSuccess();
        assertEquals(24, r.data.length, "unlock counter(4) + secret(20)");
    }

    @Test
    void setupTwiceRejected() {
        setup().requireSuccess();
        assertEquals(SW_SETUP_ALREADY_DONE, sc.send(0x2A, 0x00, 0x00, new byte[0]).sw);
    }

    @Test
    void satodimeStatusReportsKeyslots() {
        setup().requireSuccess();
        Response r = sc.send(0x50, 0x00, 0x00).requireSuccess();
        assertEquals(4 + 1 + KEYS, r.data.length);
        assertEquals(KEYS, r.data[4] & 0xFF, "number of keyslots");
    }

    @Test
    void authentikeyAvailableBeforeSetup() {
        Response r = sc.send(0xAD, 0x00, 0x00).requireSuccess();
        assertTrue(r.data.length > 32 + 4);
    }
}
