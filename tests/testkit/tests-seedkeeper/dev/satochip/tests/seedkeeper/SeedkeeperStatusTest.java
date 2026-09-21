package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedkeeperStatusTest extends AbstractSeedkeeperTest {

    @Test
    void statusLayoutBeforeSetup() {
        Response r = status().requireSuccess();
        assertEquals(13, r.data.length);
        assertEquals(0, r.data[10], "setupDone");
        assertEquals(1, r.data[11], "needs_secure_channel");
    }

    @Test
    void setupThenStatus() {
        setupDefault().requireSuccess();
        Response r = status().requireSuccess();
        assertEquals(1, r.data[10], "setupDone after setup");
        assertEquals(1, r.data[9] & 0xFF, "seedkeeper reports bip32 available");
        assertEquals(0, r.data[12] & 0xFF, "NFC policy default enabled");
    }

    @Test
    void setupTwiceRejected() {
        setupDefault().requireSuccess();
        assertEquals(SW_SETUP_ALREADY_DONE, sc.send(0x2A, 0x00, 0x00, setupData(PIN, PUK, DEFAULT_PIN, 0)).sw);
    }

    @Test
    void seedkeeperStatus() {
        setupDefault().requireSuccess();
        verifyPinOk(PIN);
        Response r = sc.send(0xA7, 0x00, 0x00).requireSuccess();
        assertEquals(17, r.data.length);
        assertEquals(0, (r.data[0] & 0xFF) << 8 | (r.data[1] & 0xFF), "no objects yet");
        assertTrue((((r.data[2] & 0xFF) << 8) | (r.data[3] & 0xFF)) > 0, "total memory");
    }
}
